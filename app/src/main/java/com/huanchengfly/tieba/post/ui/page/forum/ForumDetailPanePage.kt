package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ThreadNavTypeMap
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadViewModel
import kotlinx.serialization.Serializable

/** 贴吧双栏模式下详情面板的起始占位路由, 无选中帖子时展示. */
@Serializable
private data object ForumDetailPlaceholder

/**
 * 贴吧列表-详情双栏宿主.
 *
 * 紧凑宽度下行为与旧版一致: 列表全屏, 点击帖子通过根导航整页打开.
 * 中宽及以上时左侧渲染贴吧列表, 右侧通过嵌套 NavHost 渲染帖子详情;
 * 详情持有独立的返回栈, 返回键先关闭详情, 再退出贴吧.
 */
@Composable
fun ForumDetailPanePage(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    navigator: NavController,
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
                popUpTo<ForumDetailPlaceholder>()
            }
        }
    }

    val detailPane: @Composable () -> Unit = {
        NavHost(
            navController = detailNavController,
            startDestination = ForumDetailPlaceholder,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<ForumDetailPlaceholder> {
                ThreadDetailPlaceholder()
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

    if (isCompact) {
        if (isDetailShowing) {
            // 从双栏折叠而来: 详情继续全屏展示, 返回键先关闭详情
            detailPane()
        } else {
            ForumPage(forumName, avatarUrl, transitionKey, navigator, onOpenThread = openThread)
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 400.dp)
            ) {
                ForumPage(forumName, avatarUrl, transitionKey, navigator, onOpenThread = openThread)
            }
            VerticalDivider()
            Box(modifier = Modifier.weight(1f)) {
                detailPane()
            }
        }
    }
}

@Composable
private fun ThreadDetailPlaceholder() {
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
