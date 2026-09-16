package com.bmb.runsensewear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.bmb.runsensewear.presentation.screens.AiCoachScreen
import com.bmb.runsensewear.presentation.screens.FormSummaryScreen
import com.bmb.runsensewear.presentation.screens.HeartRateScreen
import com.bmb.runsensewear.presentation.screens.HomeScreen
import com.bmb.runsensewear.presentation.screens.PausedScreen
import com.bmb.runsensewear.presentation.screens.RunCompleteScreen
import com.bmb.runsensewear.presentation.screens.RunningScreen
import com.bmb.runsensewear.presentation.screens.SensorCheckScreen

/**
 * 화면 관리
 * 화면 파일 안에서 route 문자열을 직접 쓰지 말고 항상 이 상수를 참조
 */
object RunSenseRoutes {
    const val HOME = "home"                   // 01 메인 대기화면
    const val SENSOR_CHECK = "sensor_check"    // 02 센서 연결 확인 중
    const val RUNNING = "running"              // 03 러닝 중
    const val HEART_RATE = "heart_rate"        // 04 심박 상세
    const val FORM_SUMMARY = "form_summary"    // 05 폼 요약
    const val PAUSED = "paused"                // 07 일시정지
    const val RUN_COMPLETE = "run_complete"    // 08 러닝 완료

    /** 06 AI 코치 알림. 자동으로 닫힐지 여부를 인자로 받습니다. */
    const val ARG_AUTO_DISMISS = "auto"
    const val AI_COACH = "ai_coach?$ARG_AUTO_DISMISS={$ARG_AUTO_DISMISS}"

    /**
     * @param autoDismiss 러닝 중 코칭 이벤트로 띄울 때 true. 폼 요약에서 밀어서
     *   들어오는 마지막 페이지일 때는 false라 화면이 스스로 닫히지 않습니다.
     */
    fun aiCoach(autoDismiss: Boolean): String = "ai_coach?$ARG_AUTO_DISMISS=$autoDismiss"
}

/**
 * 워치 앱 전체 화면 흐름.
 *
 *  01 대기 --[시작]--> 02 센서확인 --[연결완료]--> 03 러닝중
 *  03 러닝중 <-- 스와이프 --> 04 심박상세 <-- 스와이프 --> 05 폼요약 <-- 스와이프 --> 06 AI코치
 *  03 러닝중 --[감지시]--> 06 AI코치 (몇 초 후 자동으로 03으로 복귀)
 *  03 러닝중 --[일시정지 버튼]--> 07 일시정지
 *  07 일시정지 --[재개]--> 03 러닝중 (뒤로가기)
 *  07 일시정지 --[종료]--> 08 러닝완료 --[확인]--> 01 대기
 *
 * WearOS 관례상 뒤로가기는 "오른쪽 스와이프로 화면을 밀어서 닫기" 제스처를
 * 쓰기 때문에, 일반 NavHost 대신 SwipeDismissableNavHost를 사용합니다.
 *
 * 각 화면 Composable은 지표 값(거리·심박·폼 점수 등)을 기본값 있는 파라미터로
 * 받습니다. ViewModel이 생기면 여기서 state를 읽어 각 화면에 넘겨주세요.
 */
@Composable
fun WearNavGraph() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = RunSenseRoutes.HOME
    ) {
        // 01 메인 대기화면
        composable(RunSenseRoutes.HOME) {
            HomeScreen(
                onStartRunClick = {
                    navController.navigate(RunSenseRoutes.SENSOR_CHECK)
                }
            )
        }

        // 02 센서 연결 확인 중
        composable(RunSenseRoutes.SENSOR_CHECK) {
            SensorCheckScreen(
                onAllSensorsConnected = {
                    // 대기화면으로 되돌아가지 않도록 스택에서 제거하고 이동
                    navController.navigate(RunSenseRoutes.RUNNING) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = false }
                    }
                }
            )
        }

        // 03 러닝 중 (메인)
        composable(RunSenseRoutes.RUNNING) {
            RunningScreen(
                onSwipeToHeartRate = {
                    navController.navigate(RunSenseRoutes.HEART_RATE)
                },
                onPauseClick = {
                    navController.navigate(RunSenseRoutes.PAUSED)
                },
                onAiCoachTriggered = {
                    // ViewModel에서 코칭 메시지 생성 이벤트가 오면 이걸 호출
                    navController.navigate(RunSenseRoutes.aiCoach(autoDismiss = true))
                }
            )
        }

        // 04 심박 상세
        composable(RunSenseRoutes.HEART_RATE) {
            HeartRateScreen(
                onSwipeToFormSummary = {
                    navController.navigate(RunSenseRoutes.FORM_SUMMARY)
                },
                onSwipeBackToRunning = {
                    navController.popBackStack()
                }
            )
        }

        // 05 폼 요약
        composable(RunSenseRoutes.FORM_SUMMARY) {
            FormSummaryScreen(
                onSwipeToAiCoach = {
                    // 마지막 페이지이므로 스스로 닫히지 않게 autoDismiss = false
                    navController.navigate(RunSenseRoutes.aiCoach(autoDismiss = false))
                },
                onSwipeBack = {
                    navController.popBackStack()
                }
            )
        }

        // 06 AI 코치 알림
        composable(
            route = RunSenseRoutes.AI_COACH,
            arguments = listOf(
                navArgument(RunSenseRoutes.ARG_AUTO_DISMISS) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { entry ->
            val autoDismiss = entry.arguments
                ?.getBoolean(RunSenseRoutes.ARG_AUTO_DISMISS) ?: true
            AiCoachScreen(
                onDismiss = {
                    navController.popBackStack()
                },
                autoDismissMillis = if (autoDismiss) 5000L else null
            )
        }

        // 07 일시정지
        composable(RunSenseRoutes.PAUSED) {
            PausedScreen(
                onResumeClick = {
                    // 러닝중 화면 상태를 그대로 유지한 채 복귀
                    navController.popBackStack()
                },
                onStopClick = {
                    navController.navigate(RunSenseRoutes.RUN_COMPLETE) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = false }
                    }
                }
            )
        }

        // 08 러닝 완료
        composable(RunSenseRoutes.RUN_COMPLETE) {
            RunCompleteScreen(
                onDoneClick = {
                    // 대기화면까지 스택을 전부 비우고 처음 상태로 복귀
                    navController.navigate(RunSenseRoutes.HOME) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}