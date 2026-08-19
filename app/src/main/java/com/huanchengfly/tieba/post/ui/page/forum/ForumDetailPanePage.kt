package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.ui.models.settings.ForumDetailMode
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ListDetailPaneHost

/**
 * 贴吧列表-详情双栏宿主.
 *
 * 中宽及以上时左侧渲染贴吧列表, 右侧渲染帖子详情; 紧凑宽度行为与旧版一致.
 * 具体显示方式由 [ForumDetailMode] 设置控制.
 */
@Composable
fun ForumDetailPanePage(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    navigator: NavController,
    initialThreadId: Long? = null,
    initialPostId: Long = 0,
) {
    val uiSettings = LocalUISettings.current
    val initialThread = remember(initialThreadId, initialPostId) {
        initialThreadId?.let { Destination.Thread(threadId = it, postId = initialPostId) }
    }

    if (uiSettings.forumDetailMode == ForumDetailMode.FULL_SCREEN) {
        // 吧内全屏打开帖子: 不经过双栏宿主
        ForumPage(forumName, avatarUrl, transitionKey, navigator)
        return
    }

    ListDetailPaneHost(
        navigator = navigator,
        startSplit = uiSettings.forumDetailMode == ForumDetailMode.IMMEDIATE_SPLIT,
        initialThread = if (uiSettings.forumDetailMode == ForumDetailMode.KEEP_DETAIL) initialThread else null,
        detailForumName = forumName,
    ) { onOpenThread ->
        ForumPage(forumName, avatarUrl, transitionKey, navigator, onOpenThread = onOpenThread)
    }
}
