package com.bmb.runsensewear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.bmb.runsensewear.data.run.RunPhase
import com.bmb.runsensewear.presentation.run.RunEvent
import com.bmb.runsensewear.presentation.run.RunViewModel
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

    /** 러닝 중 가로로 넘기는 페이지들 — 코칭 알림을 자동으로 띄워도 되는 화면 */
    val RunPages: Set<String> = setOf(RUNNING, HEART_RATE, FORM_SUMMARY)
}

/**
 * 워치 앱 전체 화면 흐름.
 *
 *  01 대기 --[시작]--> 02 센서확인 --[연결완료]--> 03 러닝중
 *  03 러닝중 <-- 스와이프 --> 04 심박상세 <-- 스와이프 --> 05 폼요약 <-- 스와이프 --> 06 AI코치
 *  03 러닝중 --[코칭 도착]--> 06 AI코치 (몇 초 후 자동으로 03으로 복귀)
 *  03 러닝중 --[일시정지 버튼]--> 07 일시정지
 *  07 일시정지 --[재개]--> 03 러닝중 (뒤로가기)
 *  07 일시정지 --[종료]--> 08 러닝완료 --[확인]--> 01 대기
 *
 * WearOS 관례상 뒤로가기는 "오른쪽 스와이프로 화면을 밀어서 닫기" 제스처를
 * 쓰기 때문에, 일반 NavHost 대신 SwipeDismissableNavHost를 사용합니다.
 *
 * 모든 지표는 [RunViewModel]의 상태에서 읽어 각 화면에 넘깁니다.
 */
@Composable
fun WearNavGraph(viewModel: RunViewModel = viewModel()) {
    val navController = rememberSwipeDismissableNavController()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            currentRoute = it.destination.route
        }
    }

    // ViewModel 이벤트 → 화면 전환
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                RunEvent.RunStarted -> navController.navigate(RunSenseRoutes.RUNNING) {
                    // 대기화면으로 되돌아가지 않도록 센서확인을 스택에서 제거
                    popUpTo(RunSenseRoutes.HOME) { inclusive = false }
                    launchSingleTop = true
                }

                is RunEvent.CoachingArrived -> {
                    // 러닝 페이지들 위에 있을 때만 알림을 띄웁니다. 이미 코치 화면이면 내용만 바뀝니다.
                    if (currentRoute in RunSenseRoutes.RunPages) {
                        navController.navigate(RunSenseRoutes.aiCoach(autoDismiss = true)) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    // 앱이 다시 떴는데 러닝이 진행 중이면(프로세스 복구·대기화면으로 밀려남 등) 바로 러닝 화면으로.
    LaunchedEffect(Unit) {
        when (ui.phase) {
            RunPhase.Running -> navController.navigate(RunSenseRoutes.RUNNING) {
                popUpTo(RunSenseRoutes.HOME) { inclusive = false }
            }

            RunPhase.Paused -> {
                navController.navigate(RunSenseRoutes.RUNNING) { popUpTo(RunSenseRoutes.HOME) { inclusive = false } }
                navController.navigate(RunSenseRoutes.PAUSED)
            }

            else -> Unit
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = RunSenseRoutes.HOME
    ) {
        // 01 메인 대기화면
        composable(RunSenseRoutes.HOME) {
            LaunchedEffect(Unit) { viewModel.refreshSensorReadiness() }
            HomeScreen(
                onStartRunClick = { navController.navigate(RunSenseRoutes.SENSOR_CHECK) },
                insolesConnected = ui.insoleStatus.bothConnected,
                heartRateReady = ui.heartRateReady,
                phoneConnected = ui.phoneConnected,
                userName = ui.userName,
                runInProgress = ui.phase == RunPhase.Running || ui.phase == RunPhase.Paused,
                onResumeRunClick = {
                    navController.navigate(RunSenseRoutes.RUNNING) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = false }
                    }
                    if (ui.phase == RunPhase.Paused) navController.navigate(RunSenseRoutes.PAUSED)
                }
            )
        }

        // 02 센서 연결 확인 중
        composable(RunSenseRoutes.SENSOR_CHECK) {
            SensorCheckScreen(
                state = ui.sensorCheck,
                onEnter = { viewModel.beginSensorCheck() }
            )
        }

        // 03 러닝 중 (메인)
        composable(RunSenseRoutes.RUNNING) {
            RunningScreen(
                onSwipeToHeartRate = { navController.navigate(RunSenseRoutes.HEART_RATE) },
                onPauseClick = {
                    viewModel.pause()
                    navController.navigate(RunSenseRoutes.PAUSED)
                },
                paceText = ui.paceText,
                paceState = ui.paceState,
                currentPaceSecPerKm = ui.currentPaceSecPerKm,
                paceReferenceSecPerKm = ui.targetPaceSecPerKm ?: ui.avgPaceSecPerKm,
                distanceKm = ui.distanceKm,
                elapsedText = ui.elapsedText,
                heartRate = ui.heartRate,
                gpsReady = ui.gpsReady
            )
        }

        // 04 심박 상세
        composable(RunSenseRoutes.HEART_RATE) {
            HeartRateScreen(
                onSwipeToFormSummary = { navController.navigate(RunSenseRoutes.FORM_SUMMARY) },
                heartRate = ui.heartRate,
                currentZone = ui.zone,
                lactateThresholdHr = ui.lthr,
                zoneShares = ui.zoneShares,
                statusLabel = ui.heartRateLabel
            )
        }

        // 05 폼 요약
        composable(RunSenseRoutes.FORM_SUMMARY) {
            FormSummaryScreen(
                onSwipeToAiCoach = {
                    // 마지막 페이지이므로 스스로 닫히지 않게 autoDismiss = false
                    navController.navigate(RunSenseRoutes.aiCoach(autoDismiss = false))
                },
                formScore = ui.formScore,
                cadence = ui.cadence,
                balancePercent = ui.balancePercent,
                insoleConnected = ui.insoleStatus.leftConnected || ui.insoleStatus.rightConnected
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
            var requesting by remember { mutableStateOf(false) }
            LaunchedEffect(ui.coaching) { requesting = false }

            AiCoachScreen(
                onDismiss = { navController.popBackStack() },
                message = ui.coaching?.text,
                focusPoint = ui.coaching?.focusPoint,
                speaking = ui.speaking,
                fromPhone = ui.coaching?.fromPhone == true,
                requesting = requesting,
                autoDismissMillis = if (autoDismiss) 6000L else null,
                // 폰 러닝 화면이 열려 있으면 폰이 코칭을 만들어 주므로 워치에서는 요청하지 않습니다.
                onRequestCoaching = if (!autoDismiss && !ui.phoneRunActive) {
                    {
                        requesting = true
                        viewModel.requestCoachingNow()
                    }
                } else null
            )
        }

        // 07 일시정지
        composable(RunSenseRoutes.PAUSED) {
            PausedScreen(
                onResumeClick = {
                    viewModel.resume()
                    // 러닝중 화면 상태를 그대로 유지한 채 복귀
                    navController.popBackStack()
                },
                onStopClick = {
                    viewModel.finish()
                    navController.navigate(RunSenseRoutes.RUN_COMPLETE) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = false }
                    }
                },
                elapsedText = ui.elapsedText,
                distanceKm = ui.distanceKm
            )
        }

        // 08 러닝 완료
        composable(RunSenseRoutes.RUN_COMPLETE) {
            RunCompleteScreen(
                onDoneClick = {
                    viewModel.done()
                    // 대기화면까지 스택을 전부 비우고 처음 상태로 복귀
                    navController.navigate(RunSenseRoutes.HOME) {
                        popUpTo(RunSenseRoutes.HOME) { inclusive = true }
                    }
                },
                distanceKm = ui.distanceKm,
                elapsedText = ui.elapsedText,
                avgHeartRate = ui.avgHeartRate,
                saveState = ui.saveState,
                phoneConnected = ui.phoneConnected,
                onOpenReportClick = { viewModel.openReportOnPhone() }
            )
        }
    }
}
