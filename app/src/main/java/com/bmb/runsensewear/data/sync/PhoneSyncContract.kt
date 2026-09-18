package com.bmb.runsensewear.data.sync

/**
 * 워치 ↔ 폰 Data Layer 프로토콜.
 * 폰 앱(RunSense_FrontEnd) 쪽 규격은 `src/wear/wearBridge.ts` 에 1:1 로 있습니다.
 *
 * 폰 앱과 이 앱은 **같은 applicationId(com.runsense)·같은 서명 키**여야
 * Wearable Data Layer 가 메시지를 서로에게 전달합니다.
 *
 * 폰이 보내는 DataItem 은 DataMap 의 "json" 키에 JSON 문자열을 담고,
 * 같은 경로의 Message 는 본문이 그 JSON 입니다. 워치는 둘 다 같은 파서로 처리합니다.
 *
 * ── 폰 → 워치 ──────────────────────────────────────────
 *  [AUTH]           { accessToken, refreshToken, userId, userName }
 *  [AUTH_CLEAR]     로그아웃 — 워치 토큰도 지웁니다
 *  [PROFILE]        { lthr, restingHeartRate, birthDate, height, weight, userName }
 *                   LTHR·안정시 심박은 서버에 없고 폰에만 있어 여기로 받습니다.
 *  [INSOLE_STATUS]  { leftConnected, rightConnected }
 *  [INSOLE_DATA]    { "left": FootData, "right": FootData }  — 러닝 중 1초마다
 *                   FootData = AI 서버 스키마 (hitmap{heel,forefoot_left,...}, pronation_type, heel_strike_rate)
 *  [RUN_PHONE]      { active, updatedAt } — 폰 러닝 화면이 열려 있는지.
 *                   true 면 워치는 기록 저장·코칭 생성을 폰에 맡기고 센서 값만 보냅니다.
 *  [COACH]          { feedbackText, focusPoint, recExercise } — 폰이 만든 코칭을 워치 화면에도 표시
 *
 * ── 워치 → 폰 ──────────────────────────────────────────
 *  [INSOLE_REQUEST] 센서 확인 화면에서 현재 인솔 상태를 다시 보내달라고 요청
 *  [RUN_STATE]      { state: started|paused|resumed|ended, elapsedSeconds, distanceKm }
 *  [RUN_LIVE]       { phase, elapsedSeconds, distanceKm, paceSecPerKm, heartRate, cadence, formScore } — 5초마다
 *  [RUN_COMPLETE]   { distanceKm, elapsedSeconds, avgPaceSecPerKm, avgHeartRate, formScore, saved, activityId }
 *  [OPEN_REPORT]    { activityId } — 완료 화면 카드를 누르면 폰 앱 리포트 열기
 */
object PhoneSyncContract {
    const val PATH_PREFIX = "/runsense"

    const val AUTH = "$PATH_PREFIX/auth"
    const val AUTH_CLEAR = "$PATH_PREFIX/auth/clear"
    const val PROFILE = "$PATH_PREFIX/profile"
    const val INSOLE_STATUS = "$PATH_PREFIX/insole/status"
    const val INSOLE_DATA = "$PATH_PREFIX/insole/data"
    const val RUN_PHONE = "$PATH_PREFIX/run/phone"
    const val COACH = "$PATH_PREFIX/coach"

    const val INSOLE_REQUEST = "$PATH_PREFIX/insole/request"
    const val RUN_STATE = "$PATH_PREFIX/run/state"
    const val RUN_LIVE = "$PATH_PREFIX/run/live"
    const val RUN_COMPLETE = "$PATH_PREFIX/run/complete"
    const val OPEN_REPORT = "$PATH_PREFIX/open_report"

    /** DataMap 안에 JSON 본문을 담는 키 */
    const val KEY_JSON = "json"
}
