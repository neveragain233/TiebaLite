package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.ui.models.settings.HabitSettingsDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ForumFabSettingsBackupTest {
    private val legacyBackup = """
        {
          "collectedDesc": false,
          "favoriteDesc": false,
          "favoriteSeeLz": true,
          "forumSortType": 0,
          "mediaDisplayMode": "STANDARD",
          "compactSingleAsGridCell": false,
          "hideReply": false,
          "hideReplyWarning": false,
          "imageLoadType": 0,
          "imageWatermarkType": 2,
          "showBothName": false,
          "stickyHeader": true,
          "videoAutoplay": true
        }
    """.trimIndent()

    @Test
    fun oldBackupKeepsQuickRefreshDisabled() {
        val restored = Json.decodeFromString<HabitSettingsDto>(legacyBackup)
        assertFalse(restored.forumFabQuickRefresh)
    }

    @Test
    fun quickRefreshSurvivesBackupRoundTrip() {
        val original = Json.decodeFromString<HabitSettingsDto>(legacyBackup)
            .copy(forumFabQuickRefresh = true)
        assertEquals(original, Json.decodeFromString<HabitSettingsDto>(Json.encodeToString(original)))
    }
}
