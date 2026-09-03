package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.LauncherIcons

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

/**
 * 底部导航栏标签显示模式
 *
 * @since 4.0.0-beta.4.3
 * */
enum class NavigationLabel {
    ALWAYS, SELECTED, NONE
}

/**
 * 吧内详情/双栏显示模式
 *
 * 单一枚举保证各模式互斥, 不会出现自相矛盾的组合.
 */
enum class ForumDetailMode {
    /** 进入吧后列表全屏, 选帖后分屏; 从详情进入新吧时保持右侧详情 (默认) */
    KEEP_DETAIL,

    /** 进入吧后列表全屏, 选帖后分屏; 从详情进入新吧时重置为列表全屏 */
    AFTER_SELECTION,

    /** 进入吧即分屏, 右侧显示占位 */
    IMMEDIATE_SPLIT,

    /** 吧内全屏打开帖子, 不使用双栏 */
    FULL_SCREEN,
}

/**
 * 详情「全屏/收起」按钮的展示样式
 */
enum class FullscreenButtonStyle {
    /** 右下角 FAB (默认) */
    FAB,

    /** 顶栏图标 */
    TOP_BAR,

    /** 不显示 */
    NONE,
}

/**
 * 回复栏紧凑模式(隐藏回贴入口)下, 点赞/更多胶囊的停靠位置
 */
enum class CompactReplyBarPosition {
    /** 右下角 (默认, 评论导航坞同层停靠在左下角) */
    RIGHT,

    /** 左下角 (评论导航坞同层停靠在右下角) */
    LEFT,
}

/**
 * 应用级常驻侧栏(平板导航)按钮组的垂直停靠位置
 */
enum class NavRailPosition {
    /** 靠顶部 */
    TOP,

    /** 垂直居中 (默认) */
    CENTER,

    /** 靠底部, 账号图标上方 */
    BOTTOM,
}

/**
 * User UI Settings
 *
 * @param appIcon 应用图标
 * @param appIconThemed 应用图标使用动态取色
 * @param bottomNavFloating 主页底部导航栏悬浮模式
 * @param bottomNavHideOnScroll 主页底部导航栏滑动隐藏
 * @param refreshExploreOnBackToTopLongPress 动态页长按回顶键同时刷新当前列表
 * @param bottomNavLabel 主页底部导航栏标签显示模式
 * @param darkAmoled 纯黑背景颜色
 * @param darkPreference 夜间模式偏好
 * @param darkenImage 夜间模式压暗缩略图
 * @param hideExplore 隐藏主页「动态」入口
 * @param reduceEffect 减弱模糊效果
 * @param reduceMotion 减弱动态效果
 * @param setupFinished 设置向导已完成
 * @param homeForumList 吧列表单列显示
 * @param showHistoryInHome 首页显示最近逛的吧
 * @param historyLongPressDelete 首页「经过贴吧」长按删除(含震动)
 * @param subPostsInDualPane 双栏进楼中楼时保持双栏(不自动全屏)
 * @param forumDetailMode 吧内详情显示方式
 * @param largeScreenDefaultSplit 大屏(非紧凑窗口)下列表页默认分栏
 * @param forumDefaultSplit 大屏下贴吧页默认分栏
 * @param foldToPortrait 折叠到外屏时自动切换竖屏(收起双列进入详情全屏)
 * @param appNavRailPosition 应用级侧栏按钮组停靠位置
 * @param fullscreenButtonStyle 详情全屏按钮样式
 * @param commentNavEnabled 帖子详情页启用评论上/下导航
 * @param commentNavSingleKey 评论导航合并为单键(到底变为回顶键, 长按切换方向)
 * @param commentNavSingleKeyHoldToTop 单键模式长按直接回顶(默认长按为切换方向)
 * @param commentNavEndHaptic 评论导航进入最后一楼时震动
 * @param compactReplyBarPosition 回复栏紧凑模式胶囊停靠位置
 * @param compactReplyBar 回复栏使用紧凑布局
 * @param compactShowCollect 紧凑模式是否显示收藏按钮
 * @param clearImageCacheOnLaunch 打开应用时清除图片缓存
 * @param keepFavoriteThreadImages 打开时清除图片缓存时保留已收藏帖子的图片缓存
 * */
@Immutable
data class UISettings(
    val appIcon: LauncherIcons = LauncherIcons.NEW_ICON,
    val appIconThemed: Boolean = false,
    val bottomNavFloating: Boolean = false,
    val bottomNavHideOnScroll: Boolean = false,
    val refreshExploreOnBackToTopLongPress: Boolean = true,
    val bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    val darkAmoled: Boolean = false,
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val darkenImage: Boolean = true,
    val hideExplore: Boolean = false,
    val reduceEffect: Boolean = false,
    val reduceMotion: Boolean = false,
    val setupFinished: Boolean = false,
    val homeForumList: Boolean = false,
    val showHistoryInHome: Boolean = true,
    val historyLongPressDelete: Boolean = true,
    val subPostsInDualPane: Boolean = true,
    val forumDetailMode: ForumDetailMode = ForumDetailMode.KEEP_DETAIL,
    val largeScreenDefaultSplit: Boolean = true,
    val forumDefaultSplit: Boolean = true,
    val foldToPortrait: Boolean = true,
    val appNavRailPosition: NavRailPosition = NavRailPosition.CENTER,
    val fullscreenButtonStyle: FullscreenButtonStyle = FullscreenButtonStyle.FAB,
    val commentNavEnabled: Boolean = true,
    val commentNavSingleKey: Boolean = false,
    val commentNavSingleKeyHoldToTop: Boolean = false,
    val commentNavEndHaptic: Boolean = true,
    val compactReplyBarPosition: CompactReplyBarPosition = CompactReplyBarPosition.RIGHT,
    val compactReplyBar: Boolean = false,
    val compactShowCollect: Boolean = true,
    val clearImageCacheOnLaunch: Boolean = false,
    val keepFavoriteThreadImages: Boolean = false,
)
