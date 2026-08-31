package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.StringRes
import com.huanchengfly.tieba.post.R

/**
 * 设置搜索里的一条可检索项.
 *
 * @param destination 跳转到的设置子页
 * @param titleRes 标题
 * @param summaryRes 可选摘要
 * @param itemKey 目标页 SegmentedPrefsScreen 里该条目的 key, 用于定位滚动(通常即 [titleRes])
 * */
data class SettingsSearchEntry(
    val destination: SettingsDestination,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int? = null,
    val itemKey: Any? = titleRes,
)

/** 全量设置索引, 供设置搜索页过滤. */
object SettingsSearchIndex {
    val all: List<SettingsSearchEntry> = listOf(
        // 主设置页分类(点击直达对应子页)
        entry(SettingsDestination.AccountManage, R.string.title_account_manage, R.string.summary_now_account),
        entry(SettingsDestination.OKSign, R.string.title_oksign, R.string.summary_settings_oksign),
        entry(SettingsDestination.BlockSettings, R.string.title_block_settings, R.string.summary_block_settings),
        entry(SettingsDestination.UI, R.string.title_settings_custom, R.string.summary_settings_custom),
        entry(SettingsDestination.Habit, R.string.title_settings_read_habit, R.string.summary_settings_habit),
        entry(SettingsDestination.Privacy, R.string.title_settings_privacy, R.string.summary_settings_privacy),
        entry(SettingsDestination.More, R.string.title_settings_more, R.string.summary_settings_more),
        entry(SettingsDestination.Backup, R.string.title_settings_backup, R.string.summary_settings_backup),
        entry(SettingsDestination.About, R.string.title_about, R.string.summary_settings_about),

        // 界面自定义
        entry(SettingsDestination.UI, R.string.title_custom_font_size),
        entry(SettingsDestination.UI, R.string.title_reduce_motion),
        entry(SettingsDestination.UI, R.string.title_settings_night_mode),
        entry(SettingsDestination.UI, R.string.title_settings_dark_amoled, R.string.summary_dark_amoled),
        entry(SettingsDestination.UI, R.string.settings_image_darken_when_night_mode),
        entry(SettingsDestination.UI, R.string.settings_app_icon),
        entry(SettingsDestination.UI, R.string.title_settings_use_themed_icon),
        entry(SettingsDestination.UI, R.string.settings_nav_floating, R.string.summary_nav_floating),
        entry(SettingsDestination.UI, R.string.settings_nav_hide_on_scroll, R.string.summary_nav_hide_on_scroll),
        entry(SettingsDestination.UI, R.string.settings_refresh_on_back_to_top_long_press, R.string.summary_refresh_on_back_to_top_long_press),
        entry(SettingsDestination.UI, R.string.settings_nav_label),
        entry(SettingsDestination.UI, R.string.settings_forum_single),
        entry(SettingsDestination.UI, R.string.settings_home_page_show_history_forum),
        entry(SettingsDestination.UI, R.string.title_hide_explore),
        entry(SettingsDestination.UI, R.string.settings_fullscreen_button_style),
        entry(SettingsDestination.UI, R.string.settings_comment_nav_enabled, R.string.summary_comment_nav_enabled),
        entry(SettingsDestination.UI, R.string.settings_compact_reply_bar, R.string.summary_compact_reply_bar),
        entry(SettingsDestination.UI, R.string.settings_compact_reply_bar_position),
        entry(SettingsDestination.UI, R.string.settings_compact_show_collect, R.string.summary_compact_show_collect),
        entry(SettingsDestination.UI, R.string.settings_forum_detail_mode),
        entry(SettingsDestination.UI, R.string.settings_large_screen_default_split, R.string.summary_large_screen_default_split),
        entry(SettingsDestination.UI, R.string.settings_forum_default_split, R.string.summary_forum_default_split),
        entry(SettingsDestination.UI, R.string.settings_app_nav_rail_position),
        entry(SettingsDestination.UI, R.string.settings_fold_to_portrait, R.string.summary_fold_to_portrait),

        // 阅读习惯
        entry(SettingsDestination.Habit, R.string.title_settings_image_watermark),
        entry(SettingsDestination.Habit, R.string.settings_media_display_mode),
        entry(SettingsDestination.Habit, R.string.settings_compact_single_as_grid),
        entry(SettingsDestination.Habit, R.string.settings_video_autoplay),
        entry(SettingsDestination.Habit, R.string.title_show_both_username_and_nickname),
        entry(SettingsDestination.Habit, R.string.title_settings_sticky_header),
        entry(SettingsDestination.Habit, R.string.settings_collect_thread_desc_sort),
        entry(SettingsDestination.Habit, R.string.title_hide_reply_warning),
        entry(SettingsDestination.Habit, R.string.title_settings_image_load_type),
        entry(SettingsDestination.Habit, R.string.title_settings_default_sort_type),
        entry(SettingsDestination.Habit, R.string.settings_collect_thread_see_lz),
        entry(SettingsDestination.Habit, R.string.title_hide_reply),

        // 黑名单
        entry(SettingsDestination.BlockSettings, R.string.settings_hide_blocked_content),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_video),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_forum),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_user),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_keyword),
        entry(SettingsDestination.BlockSettings, R.string.title_hidden_thread_list),

        // 隐私
        entry(SettingsDestination.Privacy, R.string.title_settings_app_link, R.string.summary_app_link),
        entry(SettingsDestination.Privacy, R.string.title_settings_clipboard_link),

        // 更多
        entry(SettingsDestination.Backup, R.string.settings_backup_export),
        entry(SettingsDestination.Backup, R.string.settings_backup_export_rules),
        entry(SettingsDestination.Backup, R.string.settings_backup_import),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_enabled),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_include_rules),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_interval),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_keep_count),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_directory),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_run_now),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_history),
        entry(SettingsDestination.More, R.string.title_use_webview),
        entry(SettingsDestination.More, R.string.title_settings_worker),
        entry(SettingsDestination.More, R.string.settings_update_background_download, R.string.summary_update_background_download),

        // 签到
        entry(SettingsDestination.OKSign, R.string.title_auto_sign),
        entry(SettingsDestination.OKSign, R.string.title_oksign_slow_mode),
        entry(SettingsDestination.OKSign, R.string.title_oksign_use_official_oksign),
        entry(SettingsDestination.OKSign, R.string.title_ignore_battery_optimization),

        // 账号管理
        entry(SettingsDestination.AccountManage, R.string.title_new_account),
        entry(SettingsDestination.AccountManage, R.string.title_exit_account),
        entry(SettingsDestination.AccountManage, R.string.title_modify_username),
        entry(SettingsDestination.AccountManage, R.string.title_copy_bduss),
    )

    private fun entry(
        destination: SettingsDestination,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int? = null,
    ): SettingsSearchEntry = SettingsSearchEntry(destination, titleRes, summaryRes)
}

/**
 * 搜索跳转目标: 进入 [SettingsSearchPage] 一个子页时, 由目标页消费以定位到具体条目.
 */
object SettingsSearchTarget {
    var destination: SettingsDestination? = null
    var itemKey: Any? = null

    fun set(destination: SettingsDestination, itemKey: Any?) {
        this.destination = destination
        this.itemKey = itemKey
    }

    fun clear() {
        destination = null
        itemKey = null
    }
}
