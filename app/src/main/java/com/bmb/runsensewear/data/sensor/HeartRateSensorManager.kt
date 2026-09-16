package com.bmb.runsensewear.data.sensor

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking

private const val TAG = "HeartRateSensor"

/**
 * 심박 측정에 필요한 런타임 권한.
 *
 * Wear OS 6(API 36)부터 BODY_SENSORS가 제거되고 android.permission.health.READ_HEART_RATE로
 * 대체됐습니다. 아직 SDK에 상수가 없어 문자열을 직접 씁니다.
 * 이 앱은 targetSdk 36이므로 API 36 이상에서는 반드시 새 권한을 요청해야 합니다.
 */
val HeartRatePermission: String =
    if (android.os.Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE"
    else android.Manifest.permission.BODY_SENSORS

/**
 * 워치 내장 심박 센서의 현재 상태.
 *
 * 갤럭시 워치는 손목에서 떨어지거나 측정이 흔들리면 값을 잠시 못 주기 때문에,
 * "몇 bpm인가"뿐 아니라 "왜 값이 없는가"까지 화면에서 구분해서 보여줄 수 있도록
 * 상태를 나눠 둡니다.
 */
sealed interface HeartRateState {
    /** 기기가 심박 측정을 지원하지 않거나 심박 권한이 없습니다. */
    data object Unsupported : HeartRateState

    /** 센서는 켜졌지만 아직 첫 값이 안정되지 않았습니다. */
    data object Acquiring : HeartRateState

    /** 워치를 손목에서 벗은 상태입니다. */
    data object OffBody : HeartRateState

    /** 일시적으로 측정할 수 없습니다. */
    data object Unavailable : HeartRateState

    /** 정상 측정 중. */
    data class Measuring(val bpm: Int) : HeartRateState

    /** 측정 중이면 bpm, 아니면 0 */
    val bpmOrZero: Int
        get() = (this as? Measuring)?.bpm ?: 0
}

/**
 * Health Services의 [androidx.health.services.client.MeasureClient]로
 * 갤럭시 워치(및 다른 Wear OS 워치) 내장 광학 심박 센서를 구독합니다.
 *
 * MeasureClient는 앱이 화면에 떠 있는 동안의 실시간 측정용입니다.
 * 백그라운드에서도 계속 기록해야 하는 시점이 오면 ExerciseClient로 옮겨야 합니다.
 *
 * 별도의 심박 밴드를 페어링하지 않고 워치 자체 센서만 사용합니다.
 */
class HeartRateSensorManager(context: Context) {

    private val appContext = context.applicationContext
    private val measureClient = HealthServices.getClient(appContext).measureClient

    /** 심박 권한이 허용되어 있는지 */
    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(HeartRatePermission) == PackageManager.PERMISSION_GRANTED

    /**
     * 이 기기가 심박 실시간 측정을 지원하는지.
     * Health Services가 없는 기기(에뮬레이터 일부 이미지 등)에서도 예외 없이 false를 돌려줍니다.
     */
    suspend fun isSupported(): Boolean = runCatching {
        DataType.HEART_RATE_BPM in measureClient.getCapabilities().supportedDataTypesMeasure
    }.getOrElse {
        Log.w(TAG, "심박 측정 지원 여부 조회 실패", it)
        false
    }

    /**
     * 심박 상태 스트림. 수집을 멈추면 센서 구독도 함께 해제됩니다.
     *
     * 권한이 없거나 지원하지 않는 기기에서는 [HeartRateState.Unsupported] 하나만 내보내고 끝냅니다.
     */
    fun heartRateFlow(): Flow<HeartRateState> {
        if (!hasPermission()) return flowOf(HeartRateState.Unsupported)

        return callbackFlow {
            val callback = object : MeasureCallback {
                override fun onRegistrationFailed(throwable: Throwable) {
                    Log.w(TAG, "심박 센서 등록 실패", throwable)
                    trySend(HeartRateState.Unsupported)
                }

                override fun onAvailabilityChanged(
                    dataType: DeltaDataType<*, *>,
                    availability: Availability
                ) {
                    if (availability !is DataTypeAvailability) return
                    val state = when (availability) {
                        DataTypeAvailability.AVAILABLE -> return // 실제 값은 onDataReceived로 옵니다
                        DataTypeAvailability.ACQUIRING -> HeartRateState.Acquiring
                        DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> HeartRateState.OffBody
                        else -> HeartRateState.Unavailable
                    }
                    trySend(state)
                }

                override fun onDataReceived(data: DataPointContainer) {
                    // 한 번에 여러 샘플이 올 수 있어 가장 최근 값만 씁니다.
                    val bpm = data.getData(DataType.HEART_RATE_BPM)
                        .lastOrNull()
                        ?.value
                        ?.toInt()
                        ?: return
                    // 측정이 튀면 0이 섞여 들어옵니다. 화면에 0 bpm이 뜨지 않도록 거릅니다.
                    if (bpm > 0) trySend(HeartRateState.Measuring(bpm))
                }
            }

            measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

            awaitClose {
                // awaitClose 블록은 suspend가 아니지만 해제는 반드시 끝내고 나가야 센서가 꺼집니다.
                // Health Services 공식 샘플과 같은 방식입니다.
                runBlocking {
                    runCatching {
                        measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
                    }.onFailure { Log.w(TAG, "심박 센서 해제 실패", it) }
                }
            }
        }.onStart { emit(HeartRateState.Acquiring) }
    }
}
