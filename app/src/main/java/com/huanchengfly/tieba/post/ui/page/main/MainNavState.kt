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

    /** 双栏面板的详情是否处于打开状态, 折叠后用于隐藏底栏避免遮挡 */
    var paneDetailOpen by mutableStateOf(false)

    /** 面板详情是否处于全屏状态, 供侧栏判断「先收起再跳转」 */
    var paneDetailExpanded by mutableStateOf(false)

    /** 请求收起全屏详情的计数, 侧栏点击当前 tab 时递增, 由面板宿主消费 */
    var collapsePaneDetailRequest by mutableStateOf(0)
}

val LocalMainNavState = staticCompositionLocalOf<MainNavState> { error("No MainNavState provided!") }
