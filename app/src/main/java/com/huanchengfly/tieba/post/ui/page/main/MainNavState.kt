package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用级主导航状态, 由 MainActivity 持有, MainPage 与常驻侧栏共享.
 */
@Stable
class MainNavState {
    /** 当前根页面所在的 tab, 用于侧栏高亮 */
    var currentTab by mutableStateOf<MainDestination?>(null)

    /** 侧栏请求切换的 tab, MainPage 消费后清空 */
    var requestedTab by mutableStateOf<MainDestination?>(null)
}

val LocalMainNavState = staticCompositionLocalOf<MainNavState> { error("No MainNavState provided!") }
