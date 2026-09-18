package com.bmb.runsensewear.data.run

import java.time.LocalDate
import java.time.Period

/** 나이를 아직 모를 때 쓰는 기본 최대 심박 (220 - 30세). */
const val DEFAULT_MAX_HEART_RATE = 190

/**
 * 심박존 계산기.
 *
 * - LTHR(젖산역치 심박)이 있으면 Friel 방식: Z1 <85% · Z2 85~89 · Z3 90~94 · Z4 95~99 · Z5 100%↑
 * - 없으면 최대 심박 대비: Z1 <60% · Z2 60~70 · Z3 70~80 · Z4 80~90 · Z5 90%↑
 */
data class HeartRateZones(
    val lthr: Int? = null,
    val maxHeartRate: Int = DEFAULT_MAX_HEART_RATE
) {
    fun zoneOf(bpm: Int): Int {
        if (bpm <= 0) return 0
        val lt = lthr
        return if (lt != null && lt > 0) {
            val r = bpm.toFloat() / lt
            when {
                r < 0.85f -> 1
                r < 0.90f -> 2
                r < 0.95f -> 3
                r < 1.00f -> 4
                else -> 5
            }
        } else {
            val r = bpm.toFloat() / maxHeartRate
            when {
                r < 0.60f -> 1
                r < 0.70f -> 2
                r < 0.80f -> 3
                r < 0.90f -> 4
                else -> 5
            }
        }
    }

    companion object {
        /** 프로필의 생년월일(yyyy-MM-dd)로 220 - 나이 최대 심박을 계산합니다. */
        fun fromProfile(lthr: Int?, birthDate: String?): HeartRateZones {
            val age = ageFrom(birthDate)
            val max = if (age != null && age in 5..110) 220 - age else DEFAULT_MAX_HEART_RATE
            return HeartRateZones(lthr = lthr?.takeIf { it > 0 }, maxHeartRate = max)
        }

        fun ageFrom(birthDate: String?): Int? = runCatching {
            birthDate?.let { Period.between(LocalDate.parse(it), LocalDate.now()).years }
        }.getOrNull()
    }
}
