package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

/** 导航坞中单个按钮的边长. */
private val NavDockButtonSize = 48.dp

/**
 * 帖子详情页右侧浮动导航坞.
 *
 * 聚合「上一楼 / 下一楼」评论导航与「全屏/收起」详情切换, 替代原先独立的全屏 FAB,
 * 从而避免多个浮动控件在底部互相遮挡回复栏.
 *
 * @param onPrev 上一楼
 * @param onNext 下一楼
 * @param showCommentNav 是否显示评论导航按钮(与全屏切换解耦)
 * @param hideCommentNav 滚动时是否隐藏评论导航按钮(全屏切换不隐藏)
 * @param onPrevLongPress 按住上一楼时的回调(回顶)
 * @param onToggleDetailPane 全屏/收起详情切换; 为 null 时不显示(如紧凑窗口或样式为 TOP_BAR/NONE)
 * @param detailPaneExpanded 详情当前是否全屏
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadNavigationDock(
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showCommentNav: Boolean = true,
    hideCommentNav: Boolean = false,
    onPrevLongPress: (() -> Unit)? = null,
    onToggleDetailPane: (() -> Unit)? = null,
    detailPaneExpanded: Boolean = false,
) {
    val showFullscreen = onToggleDetailPane != null
    val hasContent = (showCommentNav && !hideCommentNav) || showFullscreen
    // 没有评论导航按钮且没有全屏按键时(手机滚动隐藏), 连同外壳一起淡出, 避免右下角残留空圆形容器
    AnimatedVisibility(
        visible = hasContent,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = showCommentNav && !hideCommentNav,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 上一楼: 点击跳上一楼, 长按回顶
                        Box(
                            modifier = Modifier
                                .size(NavDockButtonSize)
                                .combinedClickable(
                                    onClick = onPrev,
                                    onLongClick = onPrevLongPress,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.title_prev_comment),
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.title_next_comment),
                            )
                        }
                    }
                }
                if (showFullscreen) {
                    IconButton(onClick = onToggleDetailPane) {
                        Icon(
                            imageVector = if (detailPaneExpanded) {
                                Icons.Rounded.FullscreenExit
                            } else {
                                Icons.Rounded.Fullscreen
                            },
                            contentDescription = stringResource(
                                id = if (detailPaneExpanded) {
                                    R.string.desc_collapse_detail
                                } else {
                                    R.string.desc_expand_detail
                                }
                            ),
                        )
                    }
                }
            }
        }
    }
}
