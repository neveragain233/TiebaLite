package com.huanchengfly.tieba.post.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadViewModel
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
 * 紧凑宽度下行为与旧版一致: 列表全屏, 点击帖子通过根导航整页打开.
 * 中宽及以上时左侧渲染 [listPane], 右侧通过嵌套 NavHost 渲染帖子详情;
 * 详情持有独立的返回栈, 返回键先关闭详情, 再返回列表.
 */
@Composable
fun ListDetailPaneHost(
    navigator: NavController,
    modifier: Modifier = Modifier,
    listPaneMaxWidth: Dp = 400.dp,
    listPane: @Composable (onOpenThread: (Destination.Thread) -> Unit) -> Unit,
) {
    val isCompact = isWindowWidthCompact()
    val detailNavController = rememberNavController()
    val detailEntry by detailNavController.currentBackStackEntryAsState()
    val isDetailShowing = detailEntry?.destination?.hasRoute<Destination.Thread>() == true

    val openThread: (Destination.Thread) -> Unit = { thread ->
        if (isCompact) {
            // 紧凑宽度: 与旧版一致, 整页打开帖子
            navigator.navigateDebounced(thread)
        } else {
            // 双栏: 清掉旧的详情 entry, 保证每次选中帖子都是新的 ViewModel
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
                        onBack = { detailNavController.popBackStack() },
                    )
                }
            }
        }
    }

    CompositionLocalProvider(LocalDetailPaneOpen provides isDetailShowing) {
        if (isCompact) {
            if (isDetailShowing) {
                // 从双栏折叠而来: 详情继续全屏展示, 返回键先关闭详情
                detailPane()
            } else {
                listPane(openThread)
            }
        } else {
            Row(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = listPaneMaxWidth)
                ) {
                    listPane(openThread)
                }
                VerticalDivider()
                Box(modifier = Modifier.weight(1f)) {
                    detailPane()
                }
            }
        }
    }
}

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
