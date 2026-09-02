package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.annotation.WorkerThread
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.HiddenThreadDao
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.ForumDetailMode
import com.huanchengfly.tieba.post.ui.models.settings.FullscreenButtonStyle
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.MediaDisplayMode
import com.huanchengfly.tieba.post.ui.models.settings.NavRailPosition
import com.huanchengfly.tieba.post.ui.models.settings.NavigationLabel
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.SettingsBackupMetadata
import com.huanchengfly.tieba.post.ui.models.settings.SettingsBackupPayload
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.models.settings.SignSettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettingsDto
import com.huanchengfly.tieba.post.ui.models.settings.UpdateSettings
import com.huanchengfly.tieba.post.ui.models.settings.UpdateSettingsDto
import com.google.android.material.color.utilities.Variant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.ui.models.settings.CompactReplyBarPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SettingsBackupUtil {
    const val SCHEMA_VERSION = 1
    const val FILE_EXTENSION = "tbsettings"

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private const val ENTRY_NAME_METADATA = "META-INF/metadata"
    private const val ENTRY_NAME_SETTINGS = "settings.json"
    private const val ENTRY_NAME_BLOCK_RULES = "block_rules.tbrules"
    private const val ENTRY_NAME_WALLPAPER = "files/background.webp"

    fun getBackupFileName(date: java.util.Date = java.util.Date()): String {
        val format = java.text.SimpleDateFormat("yy-MM-dd_HH-mm-ss", java.util.Locale.ENGLISH)
        return "TiebaLite_Settings_${format.format(date)}.$FILE_EXTENSION"
    }

    /**
     * Read backup metadata without decoding all settings.
     */
    @WorkerThread
    @Throws(IOException::class, SerializationException::class)
    fun readMetadata(input: InputStream): SettingsBackupMetadata {
        ZipInputStream(input).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == ENTRY_NAME_METADATA) {
                    return json.decodeFromString<SettingsBackupMetadata>(zipIn.readBytes().decodeToString())
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        throw IOException("Settings backup metadata not found")
    }

    @Throws(IOException::class, SerializationException::class)
    suspend fun backup(
        context: Context,
        settingsRepository: SettingsRepository,
        output: OutputStream,
        timestamp: Long,
        includeBlockRules: Boolean,
        blockDao: BlockDao,
        hiddenDao: HiddenThreadDao,
        transactionRunner: TransactionRunner,
    ): SettingsBackupMetadata = withContext(Dispatchers.IO) {
        val blockRuleCounts = if (includeBlockRules) {
            transactionRunner {
                val forums = blockDao.getForums()
                val keywords = blockDao.getAllKeywords()
                val users = blockDao.getAllUsers()
                val hidden = hiddenDao.getAllHidden()
                BlockRuleCounts(forums.size, keywords.size, users.size, hidden.size)
            }
        } else {
            BlockRuleCounts.EMPTY
        }

        var blockRulesFile: File? = null
        try {
            if (blockRuleCounts.total > 0) {
                blockRulesFile = File.createTempFile("settings_block_", ".tbrules", context.cacheDir)
                blockRulesFile.outputStream().use { stream ->
                    BlockRuleBackupUtil.backup(
                        dao = blockDao,
                        hiddenDao = hiddenDao,
                        transaction = transactionRunner,
                        timestamp = timestamp,
                        output = stream,
                    )
                }
            }

            val themeSettings = settingsRepository.themeSettings.snapshot()
            val wallpaperFile = themeSettings.transBackground
                ?.takeIf { it.isNotBlank() }
                ?.let { File(context.filesDir, it) }
                ?.takeIf { it.isFile }

            val metadata = SettingsBackupMetadata(
                schemaVersion = SCHEMA_VERSION,
                createdAt = timestamp,
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE.toLong(),
                containsBlockRules = blockRulesFile != null,
                containsWallpaper = wallpaperFile != null,
                forumRuleCount = blockRuleCounts.forumRuleCount,
                keywordRuleCount = blockRuleCounts.keywordRuleCount,
                userRuleCount = blockRuleCounts.userRuleCount,
                hiddenPostCount = blockRuleCounts.hiddenPostCount,
            )

            ZipOutputStream(output).use { zipOut ->
                zipOut.putNextEntry(ZipEntry(ENTRY_NAME_METADATA))
                zipOut.write(json.encodeToString(SettingsBackupMetadata.serializer(), metadata).encodeToByteArray())
                zipOut.closeEntry()

                zipOut.putNextEntry(ZipEntry(ENTRY_NAME_SETTINGS))
                zipOut.write(createPayload(settingsRepository))
                zipOut.closeEntry()

                blockRulesFile?.inputStream()?.use { stream ->
                    zipOut.putNextEntry(ZipEntry(ENTRY_NAME_BLOCK_RULES))
                    stream.copyTo(zipOut)
                    zipOut.closeEntry()
                }

                wallpaperFile?.inputStream()?.use { stream ->
                    zipOut.putNextEntry(ZipEntry(ENTRY_NAME_WALLPAPER))
                    stream.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
            metadata
        } finally {
            blockRulesFile?.delete()
        }
    }

    /**
     * Restore settings and/or nested block rules. Block rules use the existing replace semantics.
     */
    @Throws(IOException::class, SerializationException::class)
    suspend fun restore(
        context: Context,
        settingsRepository: SettingsRepository,
        input: InputStream,
        includeSettings: Boolean,
        includeBlockRules: Boolean,
        blockDao: BlockDao,
        hiddenDao: HiddenThreadDao,
        transactionRunner: TransactionRunner,
        restoreOption: Int = 0,
    ): Unit = withContext(Dispatchers.IO) {
        var payload: SettingsBackupPayload? = null
        var blockRulesFile: File? = null
        var wallpaperFile: File? = null

        ZipInputStream(input).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                when (entry.name) {
                    ENTRY_NAME_SETTINGS -> {
                        payload = json.decodeFromString<SettingsBackupPayload>(
                            zipIn.readBytes().decodeToString()
                        )
                    }

                    ENTRY_NAME_BLOCK_RULES -> if (includeBlockRules) {
                        val temp = File.createTempFile("settings_restore_", ".tbrules", context.cacheDir)
                        temp.outputStream().use { stream -> zipIn.copyTo(stream) }
                        blockRulesFile = temp
                    }

                    ENTRY_NAME_WALLPAPER -> if (includeSettings) {
                        val target = File(
                            context.filesDir,
                            "background_${System.currentTimeMillis()}.webp"
                        )
                        target.outputStream().use { stream -> zipIn.copyTo(stream) }
                        wallpaperFile = target
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        try {
            val currentPayload = payload
            if (includeSettings && currentPayload == null) {
                throw IOException("Settings payload not found")
            }
            if (includeBlockRules && blockRulesFile == null) {
                throw IOException("Block rules not found")
            }

            if (includeSettings && currentPayload != null) {
                restoreSettings(context, settingsRepository, currentPayload, wallpaperFile?.name)
            }
            if (includeBlockRules) {
                val temp = blockRulesFile ?: throw IOException("Block rules not found")
                temp.inputStream().use { stream ->
                    BlockRuleBackupUtil.restore(
                        dao = blockDao,
                        hiddenDao = hiddenDao,
                        transaction = transactionRunner,
                        input = stream,
                        restoreOption = restoreOption,
                    )
                }
            }
        } finally {
            blockRulesFile?.delete()
        }
    }

    private suspend fun restoreSettings(
        context: Context,
        settingsRepository: SettingsRepository,
        payload: SettingsBackupPayload,
        wallpaperName: String?,
    ) {
        payload.blockSettings?.let {
            settingsRepository.blockSettings.setNow(
                BlockSettings(blockVideo = it.blockVideo, hideBlocked = it.hideBlocked)
            )
        }
        payload.fontScale?.let { settingsRepository.fontScale.setNow(it) }
        payload.habitSettings?.let { settingsRepository.habitSettings.setNow(it.toModel()) }
        payload.privacySettings?.let {
            settingsRepository.privacySettings.setNow(
                PrivacySettings(readClipBoardLink = it.readClipBoardLink)
            )
        }
        payload.themeSettings?.let {
            settingsRepository.themeSettings.setNow(it.toModel(wallpaperName))
        }
        payload.uiSettings?.let {
            val currentUi = settingsRepository.uiSettings.snapshot()
            val newUi = it.toModel().copy(setupFinished = currentUi.setupFinished)
            settingsRepository.uiSettings.setNow(newUi)
            if (currentUi.appIcon != newUi.appIcon) {
                AppIconUtil.setIcon(newUi.appIcon, context)
            }

        }
        payload.updateSettings?.let {
            settingsRepository.updateSettings.setNow(
                UpdateSettings(backgroundDownload = it.backgroundDownload)
            )
        }
        payload.signSettings?.let {
            settingsRepository.signConfig.setNow(it.toModel())
        }
    }

    private suspend fun createPayload(settingsRepository: SettingsRepository): ByteArray {
        val block = settingsRepository.blockSettings.snapshot()
        val habit = settingsRepository.habitSettings.snapshot()
        val privacy = settingsRepository.privacySettings.snapshot()
        val theme = settingsRepository.themeSettings.snapshot()
        val ui = settingsRepository.uiSettings.snapshot()
        val update = settingsRepository.updateSettings.snapshot()
        val sign = settingsRepository.signConfig.snapshot()

        val payload = SettingsBackupPayload(
            blockSettings = BlockSettingsDto(
                blockVideo = block.blockVideo,
                hideBlocked = block.hideBlocked,
            ),
            fontScale = settingsRepository.fontScale.snapshot(),
            habitSettings = HabitSettingsDto(
                collectedDesc = habit.collectedDesc,
                favoriteDesc = habit.favoriteDesc,
                favoriteSeeLz = habit.favoriteSeeLz,
                forumSortType = habit.forumSortType,
                mediaDisplayMode = habit.mediaDisplayMode.name,
                compactSingleAsGridCell = habit.compactSingleAsGridCell,
                hideReply = habit.hideReply,
                hideReplyWarning = habit.hideReplyWarning,
                imageLoadType = habit.imageLoadType,
                imageWatermarkType = habit.imageWatermarkType,
                showBothName = habit.showBothName,
                stickyHeader = habit.stickyHeader,
                videoAutoplay = habit.videoAutoplay,
            ),
            privacySettings = PrivacySettingsDto(readClipBoardLink = privacy.readClipBoardLink),
            themeSettings = ThemeSettingsDto(
                theme = theme.theme.name,
                customColorArgb = theme.customColor?.toArgb()?.toLong(),
                customVariant = theme.customVariant?.name,
                transColorArgb = theme.transColor.toArgb().toLong(),
                transAlpha = theme.transAlpha,
                transBlur = theme.transBlur,
                transDarkColorMode = theme.transDarkColorMode,
            ),
            uiSettings = UISettingsDto(
                appIcon = ui.appIcon.name,
                appIconThemed = ui.appIconThemed,
                bottomNavFloating = ui.bottomNavFloating,
                bottomNavHideOnScroll = ui.bottomNavHideOnScroll,
                refreshExploreOnBackToTopLongPress = ui.refreshExploreOnBackToTopLongPress,
                bottomNavLabel = ui.bottomNavLabel.name,
                darkAmoled = ui.darkAmoled,
                darkPreference = ui.darkPreference.name,
                darkenImage = ui.darkenImage,
                hideExplore = ui.hideExplore,
                reduceEffect = ui.reduceEffect,
                reduceMotion = ui.reduceMotion,
                homeForumList = ui.homeForumList,
                showHistoryInHome = ui.showHistoryInHome,
                historyLongPressDelete = ui.historyLongPressDelete,
                subPostsInDualPane = ui.subPostsInDualPane,
                forumDetailMode = ui.forumDetailMode.name,
                largeScreenDefaultSplit = ui.largeScreenDefaultSplit,
                forumDefaultSplit = ui.forumDefaultSplit,
                foldToPortrait = ui.foldToPortrait,
                appNavRailPosition = ui.appNavRailPosition.name,
                fullscreenButtonStyle = ui.fullscreenButtonStyle.name,
                commentNavEnabled = ui.commentNavEnabled,
                commentNavSingleKey = ui.commentNavSingleKey,
                commentNavSingleKeyHoldToTop = ui.commentNavSingleKeyHoldToTop,
                commentNavEndHaptic = ui.commentNavEndHaptic,
                compactReplyBarPosition = ui.compactReplyBarPosition.name,
                compactReplyBar = ui.compactReplyBar,
                compactShowCollect = ui.compactShowCollect,
                clearImageCacheOnLaunch = ui.clearImageCacheOnLaunch,
            ),
            updateSettings = UpdateSettingsDto(backgroundDownload = update.backgroundDownload),
            signSettings = SignSettingsDto(
                autoSign = sign.autoSign,
                autoSignSlow = sign.autoSignSlow,
                autoSignHour = sign.autoSignTime.hourOfDay,
                autoSignMinute = sign.autoSignTime.minute,
                okSignOfficial = sign.okSignOfficial,
            ),
        )
        return json.encodeToString(SettingsBackupPayload.serializer(), payload).encodeToByteArray()
    }

    private fun HabitSettingsDto.toModel(): HabitSettings = HabitSettings(
        collectedDesc = collectedDesc,
        favoriteDesc = favoriteDesc,
        favoriteSeeLz = favoriteSeeLz,
        forumSortType = forumSortType,
        mediaDisplayMode = enumOrDefault(mediaDisplayMode, MediaDisplayMode.STANDARD),
        compactSingleAsGridCell = compactSingleAsGridCell,
        hideReply = hideReply,
        hideReplyWarning = hideReplyWarning,
        imageLoadType = imageLoadType,
        imageWatermarkType = imageWatermarkType,
        showBothName = showBothName,
        stickyHeader = stickyHeader,
        videoAutoplay = videoAutoplay,
    )

    private fun ThemeSettingsDto.toModel(wallpaperName: String?): ThemeSettings = ThemeSettings(
        theme = enumOrDefault(theme, Theme.BLUE),
        customColor = customColorArgb?.let { Color(it.toInt()) },
        customVariant = customVariant?.let { name -> Variant.entries.firstOrNull { it.name == name } },
        transColor = Color(transColorArgb.toInt()),
        transAlpha = transAlpha,
        transBlur = transBlur,
        transDarkColorMode = transDarkColorMode,
        transBackground = wallpaperName,
    )

    private fun UISettingsDto.toModel(): UISettings = UISettings(
        appIcon = enumOrDefault(appIcon, LauncherIcons.NEW_ICON),
        appIconThemed = appIconThemed,
        bottomNavFloating = bottomNavFloating,
        bottomNavHideOnScroll = bottomNavHideOnScroll,
        refreshExploreOnBackToTopLongPress = refreshExploreOnBackToTopLongPress,
        bottomNavLabel = enumOrDefault(bottomNavLabel, NavigationLabel.ALWAYS),
        darkAmoled = darkAmoled,
        darkPreference = enumOrDefault(darkPreference, DarkPreference.FOLLOW_SYSTEM),
        darkenImage = darkenImage,
        hideExplore = hideExplore,
        reduceEffect = reduceEffect,
        reduceMotion = reduceMotion,
        homeForumList = homeForumList,
        showHistoryInHome = showHistoryInHome,
        historyLongPressDelete = historyLongPressDelete,
        subPostsInDualPane = subPostsInDualPane,
        clearImageCacheOnLaunch = clearImageCacheOnLaunch,
        forumDetailMode = enumOrDefault(forumDetailMode, ForumDetailMode.KEEP_DETAIL),
        largeScreenDefaultSplit = largeScreenDefaultSplit,
        forumDefaultSplit = forumDefaultSplit,
        foldToPortrait = foldToPortrait,
        appNavRailPosition = enumOrDefault(appNavRailPosition, NavRailPosition.CENTER),
        fullscreenButtonStyle = enumOrDefault(fullscreenButtonStyle, FullscreenButtonStyle.FAB),
        commentNavEnabled = commentNavEnabled,
        commentNavSingleKey = commentNavSingleKey,
        commentNavSingleKeyHoldToTop = commentNavSingleKeyHoldToTop,
        commentNavEndHaptic = commentNavEndHaptic,
        compactReplyBarPosition = enumOrDefault(compactReplyBarPosition, CompactReplyBarPosition.RIGHT),
        compactReplyBar = compactReplyBar,
        compactShowCollect = compactShowCollect,
    )

    private fun SignSettingsDto.toModel(): SignConfig = SignConfig(
        autoSign = autoSign,
        autoSignSlow = autoSignSlow,
        autoSignTime = HmTime(hourOfDay = autoSignHour, minute = autoSignMinute),
        okSignOfficial = okSignOfficial,
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T {
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    private data class BlockRuleCounts(
        val forumRuleCount: Int,
        val keywordRuleCount: Int,
        val userRuleCount: Int,
        val hiddenPostCount: Int,
    ) {
        val total: Int
            get() = forumRuleCount + keywordRuleCount + userRuleCount + hiddenPostCount

        companion object {
            val EMPTY = BlockRuleCounts(0, 0, 0, 0)
        }
    }
}
