package com.bmb.runsensewear.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bmb.runsensewear.data.auth.TokenStore
import com.bmb.runsensewear.data.network.FootData
import com.bmb.runsensewear.data.run.InsoleSample
import com.bmb.runsensewear.data.run.InsoleStatus
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private const val TAG = "PhoneSync"

/** 폰이 보내 준 프로필 (LTHR 등 서버에 없는 값 포함) */
data class PhoneProfile(
    val lthr: Int? = null,
    val restingHeartRate: Int? = null,
    val birthDate: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val userName: String? = null
)

/** 폰이 만든 AI 코칭 문장 (음성은 폰에서 재생) */
data class PhoneCoaching(
    val feedbackText: String,
    val focusPoint: String?,
    val recExercise: String?
)

/**
 * 폰 앱과의 Data Layer 연동.
 *
 * 수신은 [PhoneDataListenerService]가 받아서 [handleDataEvents]/[handleMessage]로 넘겨주고,
 * 송신은 연결된 모든 노드(보통 폰 하나)에 MessageClient로 보냅니다.
 */
class PhoneSyncManager(
    context: Context,
    private val tokenStore: TokenStore,
    private val scope: CoroutineScope,
    private val json: Json
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val _phoneConnected = MutableStateFlow(false)
    val phoneConnected: StateFlow<Boolean> = _phoneConnected

    private val _insoleStatus = MutableStateFlow(InsoleStatus())
    val insoleStatus: StateFlow<InsoleStatus> = _insoleStatus

    private val _insoleSample = MutableStateFlow<InsoleSample?>(null)
    val insoleSample: StateFlow<InsoleSample?> = _insoleSample

    private val _phoneProfile = MutableStateFlow<PhoneProfile?>(null)
    val phoneProfile: StateFlow<PhoneProfile?> = _phoneProfile

    /** 폰 러닝 화면(DashboardA)이 열려 있는지 — 이때 워치는 기록 저장/코칭 생성을 폰에 맡깁니다. */
    private val _phoneRunActive = MutableStateFlow(false)
    val phoneRunActive: StateFlow<Boolean> = _phoneRunActive

    /** 폰 러닝 설정(거리 모드)에서 정한 목표 페이스 초/km. 없으면 null */
    private val _targetPaceSecPerKm = MutableStateFlow<Double?>(null)
    val targetPaceSecPerKm: StateFlow<Double?> = _targetPaceSecPerKm

    private val _phoneCoaching = MutableSharedFlow<PhoneCoaching>(extraBufferCapacity = 4)
    val phoneCoaching: SharedFlow<PhoneCoaching> = _phoneCoaching

    // ── 초기화 / 연결 확인 ─────────────────────────────

    /** 앱 시작 시 폰이 이미 써 둔 DataItem(토큰·프로필·인솔 상태·폰 러닝 상태)을 읽어 옵니다. */
    fun loadPersistedItems() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val uri = Uri.Builder().scheme("wear").authority("*").path(PhoneSyncContract.PATH_PREFIX).build()
                val buffer = dataClient.getDataItems(uri, DataClient.FILTER_PREFIX).await()
                buffer.use { items ->
                    items.forEach { item ->
                        val map = DataMapItem.fromDataItem(item).dataMap
                        map.getString(PhoneSyncContract.KEY_JSON)?.let { handlePayload(item.uri.path.orEmpty(), it) }
                    }
                }
            }.onFailure { Log.w(TAG, "DataItem 초기 로드 실패", it) }
            refreshConnection()
        }
    }

    /** 페어링된 폰이 지금 블루투스로 붙어 있는지 */
    suspend fun refreshConnection(): Boolean {
        val connected = runCatching {
            withTimeoutOrNull(3_000) { nodeClient.connectedNodes.await() }
                ?.any { it.isNearby } ?: false
        }.getOrElse {
            Log.w(TAG, "connectedNodes 조회 실패", it)
            false
        }
        _phoneConnected.value = connected
        return connected
    }

    // ── 수신 ────────────────────────────────────────────

    fun handleDataEvents(events: DataEventBuffer) {
        events.forEach { event ->
            val path = event.dataItem.uri.path.orEmpty()
            if (event.type == DataEvent.TYPE_DELETED) {
                when (path) {
                    PhoneSyncContract.AUTH -> tokenStore.clear()
                    PhoneSyncContract.RUN_PHONE -> _phoneRunActive.value = false
                }
                return@forEach
            }
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            map.getString(PhoneSyncContract.KEY_JSON)?.let { handlePayload(path, it) }
        }
    }

    fun handleMessage(event: MessageEvent) {
        _phoneConnected.value = true
        handlePayload(event.path, String(event.data, Charsets.UTF_8))
    }

    /** DataItem/Message 공통 파서 */
    private fun handlePayload(path: String, payload: String) {
        if (path == PhoneSyncContract.AUTH_CLEAR) {
            tokenStore.clear()
            return
        }
        val obj = parseJson(payload) ?: return
        when (path) {
            PhoneSyncContract.AUTH -> applyAuth(obj)

            PhoneSyncContract.PROFILE -> _phoneProfile.value = PhoneProfile(
                lthr = obj.int("lthr"),
                restingHeartRate = obj.int("restingHeartRate"),
                birthDate = obj.str("birthDate"),
                height = obj.dbl("height"),
                weight = obj.dbl("weight"),
                userName = obj.str("userName")
            )

            PhoneSyncContract.INSOLE_STATUS -> _insoleStatus.value = InsoleStatus(
                leftConnected = obj.bool("leftConnected") ?: false,
                rightConnected = obj.bool("rightConnected") ?: false,
                known = true
            )

            PhoneSyncContract.INSOLE_DATA -> runCatching {
                val left = obj["left"]?.let { json.decodeFromJsonElement<FootData>(it) } ?: FootData()
                val right = obj["right"]?.let { json.decodeFromJsonElement<FootData>(it) } ?: FootData()
                _insoleSample.value = InsoleSample(left, right, System.currentTimeMillis())
            }.onFailure { Log.w(TAG, "인솔 데이터 파싱 실패", it) }

            PhoneSyncContract.RUN_PHONE -> {
                _phoneRunActive.value = obj.bool("active") ?: false
                _targetPaceSecPerKm.value = obj.dbl("targetPaceSpk")?.takeIf { it > 0 }
            }

            PhoneSyncContract.COACH -> obj.str("feedbackText")?.let { text ->
                _phoneCoaching.tryEmit(
                    PhoneCoaching(text, obj.str("focusPoint"), obj.str("recExercise"))
                )
            }
        }
    }

    private fun applyAuth(obj: JsonObject) {
        val access = obj.str("accessToken")
        if (access.isNullOrBlank()) return
        tokenStore.save(
            accessToken = access,
            refreshToken = obj.str("refreshToken"),
            userId = obj["userId"]?.jsonPrimitive?.longOrNull,
            userName = obj.str("userName")
        )
        Log.i(TAG, "폰에서 로그인 토큰 수신")
    }

    private fun parseJson(payload: String): JsonObject? =
        runCatching { json.parseToJsonElement(payload).jsonObject }
            .onFailure { Log.w(TAG, "JSON 파싱 실패: $payload", it) }
            .getOrNull()

    private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" }
    private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.dbl(key: String) = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.bool(key: String) = this[key]?.jsonPrimitive?.booleanOrNull

    // ── 송신 ────────────────────────────────────────────

    fun requestInsoleStatus() = send(PhoneSyncContract.INSOLE_REQUEST, "{}")

    fun sendRunState(state: String, elapsedSeconds: Int, distanceKm: Double) = send(
        PhoneSyncContract.RUN_STATE,
        """{"state":"$state","elapsedSeconds":$elapsedSeconds,"distanceKm":$distanceKm}"""
    )

    fun sendJson(path: String, jsonText: String) = send(path, jsonText)

    private fun send(path: String, payload: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val nodes = withTimeoutOrNull(3_000) { nodeClient.connectedNodes.await() } ?: emptyList()
                _phoneConnected.value = nodes.any { it.isNearby }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8)).await()
                }
            }.onFailure { Log.w(TAG, "메시지 전송 실패: $path", it) }
        }
    }
}
