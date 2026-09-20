package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.icu.text.Transliterator
import androidx.annotation.StringRes
import com.huanchengfly.tieba.post.R

/**
 * 搜索里的一条可检索项.
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
    val keywordRes: List<Int> = emptyList(),
)

/**
 * 预处理后的搜索索引. ICU 中文转音有一定开销, 因此在搜索页内复用.
 */
data class SettingsSearchIndexedEntry(
    val entry: SettingsSearchEntry,
    internal val searchableTexts: List<SettingsSearchText>,
)

data class SettingsSearchText(
    val text: String,
    val pinyin: String,
    val pinyinInitials: String,
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
        entry(SettingsDestination.UI, R.string.title_reduce_effect),
        entry(SettingsDestination.UI, R.string.title_reduce_motion),
        entry(
            SettingsDestination.UI,
            R.string.title_settings_night_mode,
            keywords = intArrayOf(
                R.string.summary_night_mode_always,
                R.string.summary_night_mode_disabled,
                R.string.summary_night_mode_system,
            ),
        ),
        entry(SettingsDestination.UI, R.string.title_settings_dark_amoled, R.string.summary_dark_amoled),
        entry(SettingsDestination.UI, R.string.settings_image_darken_when_night_mode),
        entry(
            SettingsDestination.UI,
            R.string.settings_app_icon,
            keywords = intArrayOf(R.string.icon_new, R.string.icon_new_invert, R.string.icon_old),
        ),
        entry(SettingsDestination.UI, R.string.title_settings_use_themed_icon, R.string.tip_themed_icon_unsupported),
        entry(SettingsDestination.UI, R.string.settings_nav_floating, R.string.summary_nav_floating),
        entry(SettingsDestination.UI, R.string.settings_nav_hide_on_scroll, R.string.summary_nav_hide_on_scroll),
        entry(SettingsDestination.UI, R.string.settings_refresh_on_back_to_top_long_press, R.string.summary_refresh_on_back_to_top_long_press),
        entry(
            SettingsDestination.UI,
            R.string.settings_nav_label,
            keywords = intArrayOf(
                R.string.title_nav_label_always,
                R.string.title_nav_label_selected,
                R.string.title_nav_label_none,
            ),
        ),
        entry(SettingsDestination.UI, R.string.settings_explore_fab_follow_nav, R.string.summary_explore_fab_follow_nav),
        entry(SettingsDestination.UI, R.string.settings_forum_single),
        entry(SettingsDestination.UI, R.string.settings_home_page_show_history_forum),
        entry(SettingsDestination.UI, R.string.history_hide_followed_forums),
        entry(SettingsDestination.UI, R.string.settings_home_page_long_press_history_forum_delete),
        entry(SettingsDestination.UI, R.string.title_hide_explore),
        entry(SettingsDestination.UI, R.string.title_hide_notifications),
        entry(
            SettingsDestination.UI,
            R.string.settings_fullscreen_button_style,
            keywords = intArrayOf(
                R.string.fullscreen_button_style_fab,
                R.string.fullscreen_button_style_top_bar,
                R.string.fullscreen_button_style_none,
            ),
        ),
        entry(SettingsDestination.UI, R.string.settings_comment_nav_enabled, R.string.summary_comment_nav_enabled),
        entry(SettingsDestination.UI, R.string.settings_comment_nav_single_key, R.string.summary_comment_nav_single_key),
        entry(SettingsDestination.UI, R.string.settings_comment_nav_single_key_hold_top, R.string.summary_comment_nav_single_key_hold_top),
        entry(SettingsDestination.UI, R.string.settings_comment_nav_end_haptic, R.string.summary_comment_nav_end_haptic),
        entry(SettingsDestination.UI, R.string.settings_thread_subposts_keep_in_dual_pane),
        entry(SettingsDestination.UI, R.string.settings_compact_reply_bar, R.string.summary_compact_reply_bar),
        entry(
            SettingsDestination.UI,
            R.string.settings_compact_reply_bar_position,
            keywords = intArrayOf(
                R.string.compact_reply_bar_position_right,
                R.string.compact_reply_bar_position_left,
            ),
        ),
        entry(SettingsDestination.UI, R.string.settings_compact_show_collect, R.string.summary_compact_show_collect),
        entry(
            SettingsDestination.UI,
            R.string.settings_forum_detail_mode,
            keywords = intArrayOf(
                R.string.forum_detail_mode_keep_detail,
                R.string.forum_detail_mode_after_selection,
                R.string.forum_detail_mode_immediate_split,
                R.string.forum_detail_mode_full_screen,
            ),
        ),
        entry(SettingsDestination.UI, R.string.settings_large_screen_default_split, R.string.summary_large_screen_default_split),
        entry(SettingsDestination.UI, R.string.settings_forum_default_split, R.string.summary_forum_default_split),
        entry(
            SettingsDestination.UI,
            R.string.settings_app_nav_rail_position,
            keywords = intArrayOf(
                R.string.nav_rail_position_top,
                R.string.nav_rail_position_center,
                R.string.nav_rail_position_bottom,
            ),
        ),
        entry(SettingsDestination.UI, R.string.settings_fold_to_portrait, R.string.summary_fold_to_portrait),

        // 阅读习惯
        entry(
            SettingsDestination.Habit,
            R.string.title_settings_image_watermark,
            keywords = intArrayOf(
                R.string.title_image_watermark_none,
                R.string.title_image_watermark_user_name,
                R.string.title_image_watermark_forum_name,
            ),
        ),
        entry(
            SettingsDestination.Habit,
            R.string.settings_media_display_mode,
            keywords = intArrayOf(
                R.string.media_display_mode_hide,
                R.string.media_display_mode_compact,
                R.string.media_display_mode_standard,
            ),
        ),
        entry(SettingsDestination.Habit, R.string.settings_compact_single_as_grid, R.string.summary_compact_single_as_grid),
        entry(SettingsDestination.Habit, R.string.settings_video_autoplay),
        entry(SettingsDestination.Habit, R.string.title_show_both_username_and_nickname),
        entry(
            SettingsDestination.Habit,
            R.string.title_settings_sticky_header,
            R.string.tip_sticky_header,
            keywords = intArrayOf(
                R.string.settings_sticky_header_on,
                R.string.settings_sticky_header_off,
                R.string.summary_sticky_header_on,
                R.string.summary_sticky_header_off,
            ),
        ),
        entry(
            SettingsDestination.Habit,
            R.string.settings_collect_thread_desc_sort,
            keywords = intArrayOf(
                R.string.tip_collect_thread_desc_sort_on,
                R.string.tip_collect_thread_desc_sort,
            ),
        ),
        entry(SettingsDestination.Habit, R.string.title_hide_reply_warning),
        entry(
            SettingsDestination.Habit,
            R.string.title_settings_image_load_type,
            keywords = intArrayOf(
                R.string.title_image_load_type_smart_origin,
                R.string.title_image_load_type_smart_load,
                R.string.title_image_load_type_all_origin,
            ),
        ),
        entry(
            SettingsDestination.Habit,
            R.string.title_settings_default_sort_type,
            keywords = intArrayOf(R.string.title_sort_by_reply, R.string.title_sort_by_send),
        ),
        entry(SettingsDestination.Habit, R.string.settings_forum_fab_quick_refresh, R.string.summary_forum_fab_quick_refresh),
        entry(
            SettingsDestination.Habit,
            R.string.settings_collect_thread_see_lz,
            keywords = intArrayOf(
                R.string.tip_collect_thread_see_lz_on,
                R.string.tip_collect_thread_see_lz,
            ),
        ),
        entry(SettingsDestination.Habit, R.string.title_hide_reply),

        // 黑名单
        entry(SettingsDestination.BlockSettings, R.string.settings_hide_blocked_content),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_video, R.string.settings_block_video_summary),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_forum),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_user),
        entry(SettingsDestination.BlockSettings, R.string.settings_block_keyword),
        entry(SettingsDestination.BlockSettings, R.string.title_hidden_thread_list),

        // 隐私
        entry(SettingsDestination.Privacy, R.string.title_settings_app_link, R.string.summary_app_link),
        entry(SettingsDestination.Privacy, R.string.title_settings_clipboard_link, R.string.summary_clipboard_link),

        // 更多
        entry(SettingsDestination.Backup, R.string.settings_backup_export),
        entry(SettingsDestination.Backup, R.string.settings_backup_export_rules),
        entry(SettingsDestination.Backup, R.string.settings_backup_import),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_enabled),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_include_rules),
        entry(
            SettingsDestination.Backup,
            R.string.settings_auto_backup_interval,
            keywords = intArrayOf(
                R.string.auto_backup_interval_daily,
                R.string.auto_backup_interval_weekly,
                R.string.auto_backup_interval_monthly,
            ),
        ),
        entry(
            SettingsDestination.Backup,
            R.string.settings_auto_backup_keep_count,
            keywords = intArrayOf(
                R.string.auto_backup_keep_count_3,
                R.string.auto_backup_keep_count_7,
                R.string.auto_backup_keep_count_14,
                R.string.auto_backup_keep_count_30,
            ),
        ),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_directory),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_use_private_directory),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_run_now),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_history),
        entry(SettingsDestination.Backup, R.string.settings_auto_backup_last_run),
        entry(SettingsDestination.More, R.string.title_use_webview),
        entry(SettingsDestination.More, R.string.title_settings_worker),
        entry(
            SettingsDestination.More,
            R.string.settings_auto_update_check_interval,
            keywords = intArrayOf(
                R.string.auto_update_interval_every_launch,
                R.string.auto_update_interval_one_day,
                R.string.auto_update_interval_three_days,
                R.string.auto_update_interval_weekly,
                R.string.auto_update_interval_two_weeks,
                R.string.auto_update_interval_monthly,
            ),
        ),
        entry(SettingsDestination.More, R.string.settings_update_background_download, R.string.summary_update_background_download),
        entry(SettingsDestination.More, R.string.title_clear_image_cache_on_launch, R.string.summary_clear_image_cache_on_launch),
        entry(SettingsDestination.More, R.string.title_keep_favorite_thread_images, R.string.summary_keep_favorite_thread_images),
        entry(SettingsDestination.More, R.string.title_clear_picture_cache),

        // 签到
        entry(
            SettingsDestination.OKSign,
            R.string.title_auto_sign,
            keywords = intArrayOf(R.string.summary_auto_sign_on, R.string.summary_auto_sign),
        ),
        entry(SettingsDestination.OKSign, R.string.title_auto_sign_time),
        entry(
            SettingsDestination.OKSign,
            R.string.title_oksign_slow_mode,
            keywords = intArrayOf(R.string.summary_oksign_slow_mode_on, R.string.summary_oksign_slow_mode),
        ),
        entry(SettingsDestination.OKSign, R.string.title_oksign_use_official_oksign, R.string.summary_oksign_use_official_oksign),
        entry(
            SettingsDestination.OKSign,
            R.string.title_ignore_battery_optimization,
            keywords = intArrayOf(
                R.string.summary_ignore_battery_optimization,
                R.string.summary_battery_optimization_ignored,
            ),
        ),

        // 账号管理
        entry(SettingsDestination.AccountManage, R.string.title_switch_account, R.string.summary_now_account),
        entry(SettingsDestination.AccountManage, R.string.title_new_account),
        entry(SettingsDestination.AccountManage, R.string.title_exit_account),
        entry(SettingsDestination.AccountManage, R.string.title_modify_username),
        entry(SettingsDestination.AccountManage, R.string.title_copy_bduss, R.string.summary_copy_bduss),
        entry(SettingsDestination.AccountManage, R.string.title_my_tail, R.string.tip_no_little_tail),

        // 关于
        entry(SettingsDestination.About, R.string.settings_check_update, R.string.summary_check_update),
        entry(SettingsDestination.About, R.string.title_disclaimer),
        entry(SettingsDestination.About, R.string.about_source_code),
        entry(SettingsDestination.About, R.string.about_upstream),
        entry(SettingsDestination.About, R.string.about_license),
    )

    private val indexMutex = Mutex()
    private var cachedConfiguration: Configuration? = null
    private var cachedIndex: List<SettingsSearchIndexedEntry>? = null

    suspend fun index(context: Context): List<SettingsSearchIndexedEntry> = withContext(Dispatchers.Default) {
        indexMutex.withLock {
            val configuration = Configuration(context.resources.configuration)
            cachedIndex?.takeIf { cachedConfiguration == configuration } ?: buildIndex(context).also {
                cachedConfiguration = configuration
                cachedIndex = it
            }
        }
    }

    private fun buildIndex(context: Context): List<SettingsSearchIndexedEntry> {
        // One converter per build; never share this mutable ICU object across threads.
        val transliterator = runCatching {
            Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower")
        }.getOrNull()
        return all.map { entry ->
            val searchableValues = buildList {
                add(context.getString(entry.titleRes))
                entry.summaryRes?.let { add(context.getString(it)) }
                entry.keywordRes.forEach { add(context.getString(it)) }
            }

            SettingsSearchIndexedEntry(
                entry = entry,
                searchableTexts = searchableValues.map { value ->
                    val pinyin = toPinyin(value, transliterator)
                    SettingsSearchText(
                        text = normalizeText(value),
                        pinyin = pinyin.full,
                        pinyinInitials = pinyin.initials,
                    )
                },
            )
        }
    }

    fun search(keyword: String, index: List<SettingsSearchIndexedEntry>): List<SettingsSearchEntry> {
        val terms = keyword.lowercase()
            .split(Regex("\\s+"))
            .map { term -> term.replace(NON_WORD_REGEX, "") }
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()

        return index.mapNotNull { indexed ->
            terms.fold(0) { total, term ->
                val score = matchTerm(term, indexed)
                if (score == Int.MAX_VALUE) return@mapNotNull null
                total + score
            } to indexed.entry
        }
            .sortedWith(compareBy({ it.first }, { it.second.titleRes }))
            .map { it.second }
    }

    private fun matchTerm(term: String, indexed: SettingsSearchIndexedEntry): Int {
        return indexed.searchableTexts.mapIndexed { index, searchableText ->
            matchText(
                term,
                searchableText.text,
                searchableText.pinyin,
                searchableText.pinyinInitials,
            ).let { score ->
                if (score == Int.MAX_VALUE) score else score + index.coerceAtMost(1)
            }
        }.minOrNull() ?: Int.MAX_VALUE
    }

    private fun matchText(term: String, text: String, pinyin: String, initials: String): Int {
        text.indexOf(term).let {
            if (it >= 0) return when {
                it == 0 && text.length == term.length -> 0
                it == 0 -> 1
                else -> 2
            }
        }

        if (term.all { it.isLetterOrDigit() && it.code < 128 }) {
            pinyin.indexOf(term).let {
                if (it >= 0) return when {
                    it == 0 && pinyin.length == term.length -> 1
                    it == 0 -> 2
                    else -> 3
                }
            }

            // 首字母匹配要求至少两个字符, 减少单字母带来的误报.
            if (term.length >= 2 && initials.contains(term)) {
                return when {
                    initials == term -> 1
                    initials.startsWith(term) -> 2
                    else -> 4
                }
            }

            // 只对较长的拉丁输入做编辑距离容错, 避免两三个字母时结果过泛.
            val maxDistance = minOf(2, term.length / 4)
            if (maxDistance > 0 && fuzzyContains(text, term, maxDistance)) return 5
            if (maxDistance > 0 && fuzzyContains(pinyin, term, maxDistance)) return 6
        }

        return if (isSubsequence(text, term)) 7 else Int.MAX_VALUE
    }

    /**
     * 计算 pattern 与 text 任意子串的最小编辑距离. 只处理拉丁查询的拼写容错.
     */
    private fun fuzzyContains(text: String, pattern: String, maxDistance: Int): Boolean {
        if (pattern.isEmpty()) return true
        if (text.isEmpty()) return false

        var previous = IntArray(pattern.length + 1) { it }
        var current = IntArray(pattern.length + 1)

        for (textChar in text) {
            current[0] = 0
            for (patternIndex in pattern.indices) {
                val substitution = previous[patternIndex] +
                    if (textChar == pattern[patternIndex]) 0 else 1
                current[patternIndex + 1] = minOf(
                    current[patternIndex] + 1, // insert into pattern
                    previous[patternIndex + 1] + 1, // delete from text
                    substitution,
                )
            }
            if (current[pattern.length] <= maxDistance) return true

            val swap = previous
            previous = current
            current = swap
        }
        return false
    }

    private fun isSubsequence(text: String, pattern: String): Boolean {
        var patternIndex = 0
        for (textChar in text) {
            if (textChar == pattern[patternIndex]) {
                patternIndex++
                if (patternIndex == pattern.length) return true
            }
        }
        return false
    }

    private fun toPinyin(value: String, transliterator: Transliterator?): PinyinText {
        if (transliterator == null) return PinyinText()
        return try {
            val transliterated = transliterator
                .transliterate(value)
                .lowercase()
            val spaced = transliterated
                .replace(NON_WORD_REGEX, " ")
                .trim()
            PinyinText(
                full = spaced.replace(" ", ""),
                initials = spaced.split(' ')
                    .mapNotNull { token -> token.firstOrNull { it.isLetterOrDigit() } }
                    .joinToString(""),
            )
        } catch (_: Throwable) {
            PinyinText()
        }
    }

    private fun normalizeText(value: String): String {
        return value.lowercase().replace(NON_WORD_REGEX, "").trim()
    }

    private data class PinyinText(
        val full: String = "",
        val initials: String = "",
    )

    private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")

    private fun entry(
        destination: SettingsDestination,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int? = null,
        keywords: IntArray = intArrayOf(),
    ): SettingsSearchEntry = SettingsSearchEntry(
        destination = destination,
        titleRes = titleRes,
        summaryRes = summaryRes,
        keywordRes = keywords.toList(),
    )
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
