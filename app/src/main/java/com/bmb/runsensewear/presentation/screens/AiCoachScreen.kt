package com.bmb.runsensewear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.bmb.runsensewear.presentation.components.AutoDismissBar
import com.bmb.runsensewear.presentation.components.IconBadge
import com.bmb.runsensewear.presentation.components.PageDots
import com.bmb.runsensewear.presentation.components.RunSenseScreen
import com.bmb.runsensewear.presentation.components.VoiceWaveBars
import com.bmb.runsensewear.presentation.theme.RunSenseColors
import com.bmb.runsensewear.presentation.theme.RunSenseGradients
import com.bmb.runsensewear.presentation.theme.RunSenseWearTheme
import kotlinx.coroutines.delay

/**
 * 06 AI 코치 알림.
 *
 * 시안: AI 뱃지 + "코치" · 그라데이션 말풍선 카드 · 음성 파형 ·
 * "음성 안내 중" · 자동 닫힘 진행바 · 페이지 점 4개.
 *
 * 들어오는 경로가 두 가지입니다.
 *  - 러닝 중 코칭 이벤트로 자동 진입: 잠시 뒤 스스로 닫히고 03 러닝 중으로 복귀
 *  - 05 폼 요약에서 왼쪽으로 밀어 진입: 마지막 페이지이므로 닫지 않고 그대로 유지
 *
 * @param onDismiss 자동 복귀 콜백 (NavGraph에서 popBackStack 연결)
 * @param autoDismissMillis 자동으로 닫히기까지의 시간. null이면 닫지 않습니다.
 */
@Composable
fun AiCoachScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "페이스가 안정적이에요.\n이대로 유지하세요.",
    speaking: Boolean = true,
    autoDismissMillis: Long? = 5000L
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(autoDismissMillis) {
        if (autoDismissMillis == null) return@LaunchedEffect
        delay(autoDismissMillis)
        currentOnDismiss()
    }

    RunSenseScreen(modifier = modifier, showTime = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    IconBadge(
                        text = "AI",
                        size = 18.dp,
                        brush = RunSenseGradients.PrimaryAction,
                        shape = RoundedCornerShape(6.dp)
                    )
                    Text(
                        text = "코치",
                        style = MaterialTheme.typography.caption1,
                        color = RunSenseColors.TextTertiary
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .background(RunSenseGradients.CoachCard, RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            RunSenseColors.Purple300.copy(alpha = 0.28f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        // 한글이 어절 중간에서 끊기지 않도록 구(phrase) 단위 줄바꿈
                        style = MaterialTheme.typography.body1.merge(
                            TextStyle(lineBreak = LineBreak.Heading)
                        ),
                        color = RunSenseColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                if (speaking) {
                    VoiceWaveBars(
                        barHeight = 16.dp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "음성 안내 중",
                        style = MaterialTheme.typography.caption3,
                        color = RunSenseColors.TextTertiary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // 스와이프로 들어온 경우엔 닫히지 않으므로 진행바도 숨깁니다.
                if (autoDismissMillis != null) {
                    AutoDismissBar(
                        durationMillis = autoDismissMillis.toInt(),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(66.dp)
                    )
                }
            }

            PageDots(
                count = RUN_PAGE_COUNT,
                selectedIndex = 3,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun AiCoachScreenPreview() {
    RunSenseWearTheme { AiCoachScreen(onDismiss = {}) }
}
