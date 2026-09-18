package com.bmb.runsensewear.data.run

import androidx.compose.runtime.Immutable
import com.bmb.runsensewear.data.network.FootData
import com.bmb.runsensewear.data.network.FootHitmap
import com.bmb.runsensewear.data.sensor.HeartRateState
import kotlin.math.abs
import kotlin.math.roundToInt

/** 러닝 세션 단계 */
enum class RunPhase { Idle, Preparing, Running, Paused, Ending, Completed }

/** 페이스 상태. 시안의 "적정 페이스" 초록 뱃지에 대응합니다. */
enum class PaceState(val label: String, val apiValue: String) {
    TooSlow("페이스 느림", "Yellow"),
    Good("적정 페이스", "Green"),
    TooFast("페이스 빠름", "Red")
}

/** 1km 구간 기록 */
@Immutable
data class LapSplit(
    val lapNumber: Int,
    val paceSecPerKm: Double,
    val avgHeartRate: Int,
    val paceState: PaceState
)

/** 왼쪽/오른쪽 인솔 연결 상태 (폰 앱이 BLE로 연결해 Data Layer로 알려줍니다) */
@Immutable
data class InsoleStatus(
    val leftConnected: Boolean = false,
    val rightConnected: Boolean = false,
    /** 폰에서 한 번이라도 상태를 받았는지. false면 아직 모르는 상태입니다. */
    val known: Boolean = false
) {
    val bothConnected: Boolean get() = leftConnected && rightConnected
}

/** 폰이 보내 준 최신 족압 샘플 */
@Immutable
data class InsoleSample(
    val left: FootData = FootData(),
    val right: FootData = FootData(),
    val receivedAtMillis: Long = 0L
) {
    /** 왼발 하중 비율(%) — 50이 완벽한 균형 */
    val leftBalancePercent: Int
        get() {
            val l = left.hitmap.total
            val r = right.hitmap.total
            if (l + r == 0) return 50
            return (l * 100.0 / (l + r)).roundToInt()
        }

    val isEmpty: Boolean get() = left.hitmap.total == 0 && right.hitmap.total == 0
}

/**
 * 러닝 한 번의 실시간 상태.
 * ViewModel이 화면용 상태로 다듬기 전의 원본 값입니다.
 */
@Immutable
data class RunSession(
    val phase: RunPhase = RunPhase.Idle,
    val startedAtMillis: Long? = null,
    val elapsedSeconds: Int = 0,
    val distanceMeters: Double = 0.0,
    /** 현재 페이스 초/km. 아직 계산 전이면 null */
    val currentPaceSecPerKm: Double? = null,
    val heartRate: Int = 0,
    val heartRateState: HeartRateState = HeartRateState.Acquiring,
    val cadence: Int = 0,
    val calories: Double = 0.0,
    val gpsReady: Boolean = false,
    /** 존별 체류 샘플 수 (Z1..Z5) */
    val zoneSamples: List<Int> = List(5) { 0 },
    val heartRateSum: Long = 0L,
    val heartRateSamples: Int = 0,
    val cadenceSum: Long = 0L,
    val cadenceSamples: Int = 0,
    val laps: List<LapSplit> = emptyList(),
    /** Health Services 없이 시간만 재는 폴백 모드인지 */
    val fallbackMode: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0

    /** 평균 페이스 초/km. 100m 이상 달린 뒤부터 의미 있는 값입니다. */
    val avgPaceSecPerKm: Double?
        get() = if (distanceMeters >= 100 && elapsedSeconds > 0)
            elapsedSeconds / (distanceMeters / 1000.0) else null

    val avgHeartRate: Int
        get() = if (heartRateSamples == 0) 0 else (heartRateSum / heartRateSamples).toInt()

    val avgCadence: Int
        get() = if (cadenceSamples == 0) 0 else (cadenceSum / cadenceSamples).toInt()

    val currentZone: Int
        get() = zoneSamples.indices.maxByOrNull { zoneSamples[it] }
            ?.let { if (zoneSamples[it] == 0) 0 else it + 1 } ?: 0

    /** 존별 체류 비율 0~1. 가장 오래 머문 존이 1이 되도록 정규화합니다. */
    val zoneShares: List<Float>
        get() {
            val peak = zoneSamples.maxOrNull() ?: 0
            return if (peak == 0) List(5) { 0f } else zoneSamples.map { it.toFloat() / peak }
        }

    val isActive: Boolean get() = phase == RunPhase.Running || phase == RunPhase.Paused
}

// ── 계산 유틸 ──────────────────────────────────────────

/** 초/km → `5'28"` */
fun formatPace(secPerKm: Double?): String {
    if (secPerKm == null || secPerKm <= 0 || secPerKm.isNaN() || secPerKm > 3599) return "--'--\""
    val total = secPerKm.roundToInt()
    return "%d'%02d\"".format(total / 60, total % 60)
}

/** 백엔드 저장용 페이스 문자열 `5'28` (예시 형식 그대로 닫는 따옴표 없음) */
fun formatPaceForApi(secPerKm: Double?): String {
    if (secPerKm == null || secPerKm <= 0 || secPerKm.isNaN()) return "0'00"
    val total = secPerKm.roundToInt().coerceAtMost(59 * 60 + 59)
    return "%d'%02d".format(total / 60, total % 60)
}

/** 초 단위 경과시간을 mm:ss 로 (한 시간 넘으면 h:mm:ss) */
fun formatElapsed(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 백엔드 저장용 `HH:mm:ss` */
fun formatElapsedForApi(totalSeconds: Int): String =
    "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)

/**
 * 페이스 상태 — 목표 페이스가 없으므로 세션 평균 대비 ±8%로 판단합니다.
 * 200m를 넘기 전에는 평균이 흔들려서 항상 "적정"으로 둡니다.
 */
fun paceStateOf(
    currentSecPerKm: Double?,
    avgSecPerKm: Double?,
    distanceMeters: Double,
    targetSecPerKm: Double? = null
): PaceState {
    if (currentSecPerKm == null) return PaceState.Good
    // 폰 러닝 설정에서 목표 페이스를 받았으면 그 기준으로, 아니면 세션 평균 기준으로 판단합니다.
    val reference = targetSecPerKm?.takeIf { it > 0 }
        ?: avgSecPerKm?.takeIf { distanceMeters >= 200 }
        ?: return PaceState.Good
    val ratio = currentSecPerKm / reference
    return when {
        ratio > 1.08 -> PaceState.TooSlow
        ratio < 0.92 -> PaceState.TooFast
        else -> PaceState.Good
    }
}

/**
 * 폼 점수 0~100.
 *
 * - 케이던스: 170~185 spm이 만점, 1 spm 벗어날 때마다 1.2점 감점 (최저 20점) (40%)
 * - 좌우 균형: 50:50에서 1%p 벗어날 때마다 4점 감점 (35%)
 * - 뒤꿈치 착지율: 60% 초과분만큼 감점 (25%)
 *
 * 인솔 데이터가 없으면 케이던스만으로 계산합니다.
 */
fun formScoreOf(cadence: Int, insole: InsoleSample?): Int {
    val cadenceScore = when {
        cadence <= 0 -> 0
        cadence in 170..185 -> 100.0
        cadence < 170 -> (100 - (170 - cadence) * 1.2).coerceAtLeast(20.0)
        else -> (100 - (cadence - 185) * 1.5).coerceAtLeast(20.0)
    }.toDouble()

    if (insole == null || insole.isEmpty) return cadenceScore.roundToInt()

    val balanceScore = (100 - abs(50 - insole.leftBalancePercent) * 4).coerceAtLeast(0).toDouble()
    val heel = listOfNotNull(insole.left.heelStrikeRate, insole.right.heelStrikeRate)
        .takeIf { it.isNotEmpty() }?.average() ?: 60.0
    val heelScore = (100.0 - (heel - 60.0).coerceAtLeast(0.0)).coerceIn(0.0, 100.0)

    return (cadenceScore * 0.40 + balanceScore * 0.35 + heelScore * 0.25).roundToInt()
}

/** 히트맵 5영역 평균 (러닝 완료 시 서버 저장용) */
fun averageHitmap(samples: List<FootHitmap>): FootHitmap {
    if (samples.isEmpty()) return FootHitmap()
    val n = samples.size.toDouble()
    return FootHitmap(
        heel = (samples.sumOf { it.heel } / n).roundToInt(),
        forefootLeft = (samples.sumOf { it.forefootLeft } / n).roundToInt(),
        forefootRight = (samples.sumOf { it.forefootRight } / n).roundToInt(),
        midfootLeft = (samples.sumOf { it.midfootLeft } / n).roundToInt(),
        midfootRight = (samples.sumOf { it.midfootRight } / n).roundToInt()
    )
}
