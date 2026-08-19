package com.huanchengfly.tieba.post.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadViewModel
import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.Serializable

/**
 * 详情面板是否处于打开状态. 列表页面据此在详情关闭时重新消费
 * ThreadPage 写入的点赞/收藏结果, 保证双栏模式下列表即时刷新.
 */
@PublishedApi
internal val LocalDetailPaneOpen = compositionLocalOf { false }

/** 双栏模式下详情面板的起始占位路由, 无选中帖子时展示. */
@Serializable
private data object ListDetailPanePlaceholder

/**
 * 列表-详情双栏宿主, 供贴吧、信息流、通知等列表页面复用.
 *
 * 未选中帖子时列表保持全屏; 选中帖子后进入分屏(左列表右详情);
 * 详情顶栏可展开/收起为全屏. 紧凑宽度下行为与旧版一致: 列表全屏,
 * 点击帖子通过根导航整页打开.
 *
 * 详情 NavHost 始终在同一个组合位置, 避免移动位置导致 setGraph 重建
 * 而清空嵌套返回栈, 保证折叠/展开切换时详情状态不丢.
 */
@Composable
fun ListDetailPaneHost(
    navigator: NavController,
    modifier: Modifier = Modifier,
    listPane: @Composable (onOpenThread: (Destination.Thread) -> Unit) -> Unit,
) {
    val isCompact = isWindowWidthCompact()
    val detailNavController = rememberNavController()
    val detailEntry by detailNavController.currentBackStackEntryAsState()
    val isDetailShowing = detailEntry?.destination?.hasRoute<Destination.Thread>() == true
    var detailExpanded by rememberSaveable { mutableStateOf(false) }

    // 面板内的内容按紧凑宽度排版: 避免双栏窄栏里图文仍走宽屏分支
    // (媒体只占 50% 宽), 高度类别保持真实窗口值.
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val paneAdaptiveInfo = remember(windowAdaptiveInfo) {
        WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(0, windowAdaptiveInfo.windowSizeClass.minHeightDp),
            windowPosture = windowAdaptiveInfo.windowPosture,
        )
    }

    val openThread: (Destination.Thread) -> Unit = { thread ->
        if (isCompact) {
            // 紧凑宽度: 与旧版一致, 整页打开帖子
            navigator.navigateDebounced(thread)
        } else {
            // 清掉旧的详情 entry, 保证每次选中帖子都是新的 ViewModel
            detailNavController.navigate(thread) {
                popUpTo<ListDetailPanePlaceholder>()
            }
        }
    }

    val detailPane: @Composable () -> Unit = {
        NavHost(
            navController = detailNavController,
            startDestination = ListDetailPanePlaceholder,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<ListDetailPanePlaceholder> {
                ListDetailPanePlaceholderContent()
            }
            composable<Destination.Thread>(typeMap = ThreadNavTypeMap) { backStackEntry ->
                with(backStackEntry.toRoute<Destination.Thread>()) {
                    val vm: ThreadViewModel = hiltViewModel()
                    ThreadPage(
                        threadId = threadId,
                        postId = postId,
                        extra = from,
                        navigator = navigator,
                        viewModel = vm,
                        detailPaneExpanded = detailExpanded,
                        onToggleDetailPane = if (isCompact) null else {
                            { detailExpanded = !detailExpanded }
                        },
                        onBack = {
                            // 全屏或分屏下返回都直接关闭详情回列表全屏
                            detailExpanded = false
                            detailNavController.popBackStack()
                        },
                    )
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalWindowAdaptiveInfo provides paneAdaptiveInfo,
        LocalDetailPaneOpen provides isDetailShowing,
    ) {
        Box(modifier = modifier) {
            // 列表层
            when {
                isCompact && isDetailShowing -> Unit
                !isDetailShowing -> listPane(openThread)
                detailExpanded -> Unit
                else -> {
                    // 分屏: 列表占左半
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ListPaneFraction)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                    ) {
                        listPane(openThread)
                        VerticalDivider(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
            // 详情层: 固定组合位置, 仅切换尺寸与对齐
            val detailModifier = when {
                !isDetailShowing -> Modifier.size(0.dp)
                isCompact || detailExpanded -> Modifier.fillMaxSize()
                else -> {
                    // 分屏: 详情占右半
                    Modifier
                        .fillMaxWidth(1f - ListPaneFraction)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                }
            }
            Box(modifier = detailModifier) {
                detailPane()
            }
        }
    }
}

/** 分屏时列表栏占窗口宽度比例. */
private const val ListPaneFraction = 0.45f

@Composable
private fun ListDetailPanePlaceholderContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.tip_select_thread_to_view_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
