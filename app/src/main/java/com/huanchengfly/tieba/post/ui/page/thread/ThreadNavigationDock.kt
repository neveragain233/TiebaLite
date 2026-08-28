package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.theme.compose.withNonNull
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.R

/** 导航坞中单个按钮的边长. */
private val NavDockButtonSize = 48.dp

/** 导航坞外壳圆角, 与紧凑回复栏胶囊一致. */
private val NavDockContainerShape = RoundedCornerShape(24.dp)

/** 导航坞外壳阴影高度, 与紧凑回复栏一致. */
private val NavDockShadowElevation = 6.dp

/**
 * 帖子详情页右侧浮动导航坞.
 *
 * 聚合「上一楼 / 下一楼」评论导航与「全屏/收起」详情切换.
 *
 * @param horizontal 紧凑回复栏模式: 上下楼键横排, 与紧凑栏同高同配色, 全屏键排末尾
 * @param singleKey 单键模式: 单击沿方向推进, 到底变为回顶键
 * @param navDirection 单键模式当前方向(决定图标与语义)
 * @param atEnd 单键模式: 已推进到底, 键切换为「回顶」
 * @param holdToTop 单键模式长按直接回顶(否则长按切换方向)
 * @param onAdvance 单键模式: 单击推进(到底时为回顶)
 * @param onReverse 单键模式: 长按切换方向
 * @param onJumpToTop 单键模式长按回顶(holdToTop 时使用)
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
    horizontal: Boolean = false,
    singleKey: Boolean = false,
    navDirection: CommentNavDirection = CommentNavDirection.NEXT,
    atEnd: Boolean = false,
    holdToTop: Boolean = false,
    onAdvance: (() -> Unit)? = null,
    onReverse: (() -> Unit)? = null,
    onJumpToTop: (() -> Unit)? = null,
    showCommentNav: Boolean = true,
    hideCommentNav: Boolean = false,
    onPrevLongPress: (() -> Unit)? = null,
    onToggleDetailPane: (() -> Unit)? = null,
    detailPaneExpanded: Boolean = false,
) {
    val showFullscreen = onToggleDetailPane != null
    val navVisible = showCommentNav && !hideCommentNav
    // 没有评论导航按钮且没有全屏按键时(手机滚动隐藏), 连同外壳一起淡出, 避免角落残留空容器
    AnimatedVisibility(
        visible = navVisible || showFullscreen,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // 与紧凑回复栏同源配色: primaryContainer, 非半透明主题且未减弱效果时降低不透明度
        val containerColor = MaterialTheme.colorScheme.primaryContainer.let {
            if (!MaterialTheme.colorScheme.isTranslucent &&
                !LocalUISettings.current.reduceEffect
            ) {
                it.copy(alpha = 0.7f)
            } else {
                it
            }
        }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.shadowElevation = NavDockShadowElevation.toPx()
                    this.shape = NavDockContainerShape
                    this.clip = true
                }
                .withNonNull(LocalHazeState.current) { Modifier.defaultHazeEffect() }
                .background(color = containerColor, shape = NavDockContainerShape),
        ) {
            ProvideContentColor(MaterialTheme.colorScheme.onPrimaryContainer) {
                val navContent: @Composable () -> Unit = {
                    if (singleKey) {
                        if (atEnd) {
                            // 已到底: 键变为「回顶」, 单击回到列表顶部重新开始
                            NavDockButton(
                                icon = Icons.Rounded.VerticalAlignTop,
                                contentDescription = stringResource(R.string.btn_back_to_top),
                                onClick = onAdvance ?: {},
                            )
                        } else {
                            val advanceNext = navDirection == CommentNavDirection.NEXT
                            NavDockButton(
                                icon = if (advanceNext) {
                                    Icons.Rounded.KeyboardArrowDown
                                } else {
                                    Icons.Rounded.KeyboardArrowUp
                                },
                                contentDescription = stringResource(
                                    id = if (advanceNext) {
                                        R.string.title_next_comment
                                    } else {
                                        R.string.title_prev_comment
                                    }
                                ),
                                onClick = onAdvance ?: {},
                                onLongClick = if (holdToTop) onJumpToTop else onReverse,
                            )
                        }
                    } else {
                        NavDockButton(
                            icon = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.title_prev_comment),
                            onClick = onPrev,
                            onLongClick = onPrevLongPress,
                        )
                        NavDockButton(
                            icon = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.title_next_comment),
                            onClick = onNext,
                        )
                    }
                }
                if (horizontal) {
                    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                        AnimatedVisibility(
                            visible = navVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Row { navContent() }
                        }
                        if (showFullscreen) {
                            NavDockButton(
                                icon = if (detailPaneExpanded) {
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
                                onClick = onToggleDetailPane,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AnimatedVisibility(
                            visible = navVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                navContent()
                            }
                        }
                        if (showFullscreen) {
                            NavDockButton(
                                icon = if (detailPaneExpanded) {
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
                                onClick = onToggleDetailPane,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 导航坞按钮: 点击跳转, 可选长按(回顶). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavDockButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(NavDockButtonSize)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}
