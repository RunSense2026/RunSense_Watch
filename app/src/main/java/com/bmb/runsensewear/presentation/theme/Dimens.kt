package com.bmb.runsensewear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 작은 워치인지 (갤럭시 워치 5 40mm = 198dp, 에뮬레이터 Small Round = 192dp).
 * 44mm(225dp)·XL(240dp)에서는 false. 화면들은 이 값으로 큰 숫자·버튼 크기를 한 단계 줄입니다.
 */
@Composable
fun isCompactWatch(): Boolean = LocalConfiguration.current.screenHeightDp < 210
