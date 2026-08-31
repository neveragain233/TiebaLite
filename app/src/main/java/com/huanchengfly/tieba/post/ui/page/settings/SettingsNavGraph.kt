package com.huanchengfly.tieba.post.ui.page.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.ForumBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.HiddenPostListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.KeywordBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.UserBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage
import kotlinx.serialization.Serializable

sealed interface SettingsDestination {

    @Serializable
    object Settings: SettingsDestination

    @Serializable
    object SettingsSearch: SettingsDestination

    @Serializable
    object About: SettingsDestination

    @Serializable
    object AccountManage: SettingsDestination

    @Serializable
    object AppFont: SettingsDestination

    @Serializable
    object BlockSettings: SettingsDestination

    @Serializable
    object ForumBlockList: SettingsDestination

    @Serializable
    object KeywordBlockList: SettingsDestination

    @Serializable
    object UserBlockList: SettingsDestination

    @Serializable
    object HiddenPostList: SettingsDestination

    @Serializable
    object UI: SettingsDestination

    @Serializable
    object Habit: SettingsDestination

    @Serializable
    object Privacy: SettingsDestination

    @Serializable
    object StickyHeader: SettingsDestination

    @Serializable
    object More: SettingsDestination

    @Serializable
    object Backup: SettingsDestination

    @Serializable
    object OKSign: SettingsDestination

    @Serializable
    object WorkInfo: SettingsDestination
}

fun NavGraphBuilder.settingsGraph(navController: NavController, settingsRepo: SettingsRepository) {
    composable<SettingsDestination.Settings> {
        SettingsPage(navController)
    }

    composable<SettingsDestination.SettingsSearch> {
        SettingsSearchPage(navController)
    }

    composable<SettingsDestination.About> {
        AboutPage(
            onBack = navController::navigateUp,
            updateSettings = settingsRepo.updateSettings,
        )
    }

    composable<SettingsDestination.AccountManage> {
        AccountManagePage(myLittleTailSettings = settingsRepo.myLittleTail, navController)
    }

    composable<SettingsDestination.AppFont> {
        AppFontPage(navController::navigateUp)
    }

    composable<SettingsDestination.BlockSettings> {
        BlockSettingsPage(settings = settingsRepo.blockSettings, navController)
    }

    composable<SettingsDestination.ForumBlockList> {
        ForumBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.KeywordBlockList> {
        KeywordBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.UserBlockList> {
        UserBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.HiddenPostList> {
        HiddenPostListPage(
            onBack = navController::navigateUp,
            onOpenThread = { hidden ->
                navController.navigateDebounced(Destination.Thread(threadId = hidden.tid))
            },
        )
    }

    composable<SettingsDestination.UI> {
        UISettingsPage(settings = settingsRepo.uiSettings, navController)
    }

    composable<SettingsDestination.Habit> {
        HabitSettingsPage(
            habitSettings = settingsRepo.habitSettings,
            onStickyHeaderClicked = {
                navController.navigateDebounced(SettingsDestination.StickyHeader)
            },
            onBack = navController::navigateUp
        )
    }

    composable<SettingsDestination.Privacy> {
        PrivacySettingsPage(settingsRepo.privacySettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.StickyHeader> {
        StickyHeaderSettingsPage(settingsRepo.habitSettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.More> {
        MoreSettingsPage(
            navigator = navController,
            updateSettings = settingsRepo.updateSettings,
        )
    }

    composable<SettingsDestination.OKSign> {
        OKSignSettingsPage(settings = settingsRepo.signConfig, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.Backup> {
        SettingsBackupPage(settings = settingsRepo.backupSettings, navController)
    }

    composable<SettingsDestination.WorkInfo> {
        WorkInfoPage(onBack = navController::navigateUp)
    }
}
