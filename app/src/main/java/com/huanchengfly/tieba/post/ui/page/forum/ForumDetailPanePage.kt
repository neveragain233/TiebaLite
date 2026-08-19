package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.ui.page.ListDetailPaneHost

/**
 * 贴吧列表-详情双栏宿主.
 *
 * 中宽及以上时左侧渲染贴吧列表, 右侧渲染帖子详情; 紧凑宽度行为与旧版一致.
 */
@Composable
fun ForumDetailPanePage(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    navigator: NavController,
) {
    ListDetailPaneHost(navigator = navigator) { onOpenThread ->
        ForumPage(forumName, avatarUrl, transitionKey, navigator, onOpenThread = onOpenThread)
    }
}
