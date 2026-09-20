package com.huanchengfly.tieba.post.ui.page.settings

import com.huanchengfly.tieba.post.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchIndexTest {

    @Test
    fun indexContainsEveryPreviouslyMissingSettingsRow() {
        val indexedTitles = SettingsSearchIndex.all.map { it.titleRes }.toSet()
        val requiredTitles = setOf(
            R.string.title_reduce_effect,
            R.string.settings_explore_fab_follow_nav,
            R.string.settings_home_page_long_press_history_forum_delete,
            R.string.title_hide_notifications,
            R.string.settings_comment_nav_single_key,
            R.string.settings_comment_nav_single_key_hold_top,
            R.string.settings_comment_nav_end_haptic,
            R.string.settings_thread_subposts_keep_in_dual_pane,
            R.string.title_auto_sign_time,
            R.string.title_switch_account,
            R.string.title_my_tail,
            R.string.settings_auto_update_check_interval,
            R.string.title_clear_picture_cache,
            R.string.settings_auto_backup_use_private_directory,
            R.string.settings_auto_backup_last_run,
            R.string.settings_check_update,
            R.string.title_disclaimer,
            R.string.about_source_code,
            R.string.about_upstream,
            R.string.about_license,
        )

        assertTrue(indexedTitles.containsAll(requiredTitles))
    }

    @Test
    fun entriesHaveUniqueDestinationsAndTitles() {
        val entryIds = SettingsSearchIndex.all.map { it.destination to it.titleRes }

        assertEquals(entryIds.distinct().size, entryIds.size)
    }

    @Test
    fun entriesHaveStableNavigationKeys() {
        assertTrue(SettingsSearchIndex.all.all { it.itemKey != null })
    }

    @Test
    fun listOptionsAreSearchable() {
        val entriesByTitle = SettingsSearchIndex.all.associateBy { it.titleRes }

        assertTrue(
            entriesByTitle.getValue(R.string.settings_auto_update_check_interval)
                .keywordRes.contains(R.string.auto_update_interval_weekly)
        )
        assertTrue(
            entriesByTitle.getValue(R.string.settings_nav_label)
                .keywordRes.contains(R.string.title_nav_label_always)
        )
        assertTrue(
            entriesByTitle.getValue(R.string.settings_forum_detail_mode)
                .keywordRes.contains(R.string.forum_detail_mode_full_screen)
        )
    }
}
