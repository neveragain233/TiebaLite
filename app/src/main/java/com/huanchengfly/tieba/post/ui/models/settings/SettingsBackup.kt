package com.huanchengfly.tieba.post.ui.models.settings

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackupMetadata(
    val schemaVersion: Int,
    val createdAt: Long,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val containsBlockRules: Boolean = false,
    val containsWallpaper: Boolean = false,
    val forumRuleCount: Int = 0,
    val keywordRuleCount: Int = 0,
    val userRuleCount: Int = 0,
    val hiddenPostCount: Int = 0,
)

@Serializable
data class SettingsBackupPayload(
    val blockSettings: BlockSettingsDto? = null,
    val fontScale: Float? = null,
    val habitSettings: HabitSettingsDto? = null,
    val privacySettings: PrivacySettingsDto? = null,
    val themeSettings: ThemeSettingsDto? = null,
    val uiSettings: UISettingsDto? = null,
    val updateSettings: UpdateSettingsDto? = null,
    val signSettings: SignSettingsDto? = null,
)

@Serializable
data class BlockSettingsDto(
    val blockVideo: Boolean,
    val hideBlocked: Boolean,
)

@Serializable
data class HabitSettingsDto(
    val collectedDesc: Boolean,
    val favoriteDesc: Boolean,
    val favoriteSeeLz: Boolean,
    val forumSortType: Int,
    val mediaDisplayMode: String,
    val compactSingleAsGridCell: Boolean,
    val hideReply: Boolean,
    val hideReplyWarning: Boolean,
    val imageLoadType: Int,
    val imageWatermarkType: Int,
    val showBothName: Boolean,
    val stickyHeader: Boolean,
    val videoAutoplay: Boolean,
)

@Serializable
data class PrivacySettingsDto(
    val readClipBoardLink: Boolean,
)

@Serializable
data class ThemeSettingsDto(
    val theme: String,
    val customColorArgb: Long? = null,
    val customVariant: String? = null,
    val transColorArgb: Long,
    val transAlpha: Float,
    val transBlur: Float,
    val transDarkColorMode: Boolean,
)

@Serializable
data class UISettingsDto(
    val appIcon: String,
    val appIconThemed: Boolean,
    val bottomNavFloating: Boolean,
    val bottomNavHideOnScroll: Boolean,
    val refreshExploreOnBackToTopLongPress: Boolean,
    val bottomNavLabel: String,
    val darkAmoled: Boolean,
    val darkPreference: String,
    val darkenImage: Boolean,
    val hideExplore: Boolean,
    val reduceEffect: Boolean,
    val reduceMotion: Boolean,
    val homeForumList: Boolean,
    val showHistoryInHome: Boolean,
    val historyLongPressDelete: Boolean,
    val subPostsInDualPane: Boolean,
    val forumDetailMode: String,
    val largeScreenDefaultSplit: Boolean,
    val forumDefaultSplit: Boolean,
    val foldToPortrait: Boolean,
    val appNavRailPosition: String,
    val fullscreenButtonStyle: String,
    val commentNavEnabled: Boolean,
    val commentNavSingleKey: Boolean,
    val commentNavSingleKeyHoldToTop: Boolean,
    val commentNavEndHaptic: Boolean = true,
    val compactReplyBarPosition: String,
    val compactReplyBar: Boolean,
    val compactShowCollect: Boolean,
    val clearImageCacheOnLaunch: Boolean = false,
    val keepFavoriteThreadImages: Boolean = false,
)

@Serializable
data class UpdateSettingsDto(
    val backgroundDownload: Boolean,
    val autoUpdateCheckInterval: String? = null,
)

@Serializable
data class SignSettingsDto(
    val autoSign: Boolean,
    val autoSignSlow: Boolean,
    val autoSignHour: Int,
    val autoSignMinute: Int,
    val okSignOfficial: Boolean,
)

enum class AutoBackupInterval(val days: Int) {
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30),
}

@Immutable
data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val autoBackupIncludeRules: Boolean = true,
    val autoBackupInterval: AutoBackupInterval = AutoBackupInterval.DAILY,
    val autoBackupKeepCount: Int = 7,
    val autoBackupDirectoryUri: String? = null,
    val lastAutoBackupAt: Long = 0,
    val lastAutoBackupSucceeded: Boolean? = null,
    val lastAutoBackupMessage: String? = null,
)

data class BackupHistoryItem(
    val uri: Uri,
    val name: String,
    val lastModified: Long,
)
