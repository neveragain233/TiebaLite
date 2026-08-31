package com.huanchengfly.tieba.post.ui.page.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.AutoBackupInterval
import com.huanchengfly.tieba.post.ui.models.settings.BackupHistoryItem
import com.huanchengfly.tieba.post.ui.models.settings.BackupSettings
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.workManager
import com.huanchengfly.tieba.post.utils.SettingsBackupUtil
import com.huanchengfly.tieba.post.workers.SettingsBackupWorker
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsBackupPage(
    settings: Settings<BackupSettings>,
    navigator: NavController,
    viewModel: SettingsBackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()
    val restoreDialogState = rememberDialogState()
    var includeRuleExport by rememberSaveable { mutableStateOf(false) }

    viewModel.uiEvent.collectUiEventWithLifecycle { event ->
        val message = when (event) {
            is SettingsBackupUiEvent.BadBackup -> getString(R.string.toast_bad_backup_rule)
            is SettingsBackupUiEvent.UnsupportedBackup -> getString(R.string.toast_unsupported_backup)
            is SettingsBackupUiEvent.BackupFailed -> event.message
            is SettingsBackupUiEvent.BackupCompleted -> getString(R.string.toast_settings_backup_done)
            is SettingsBackupUiEvent.RestoreCompleted -> getString(R.string.toast_settings_restore_done)
            else -> event.toString()
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onBackup(uri, System.currentTimeMillis(), includeRuleExport)
        }
        includeRuleExport = false
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onRestoreFilePicked(uri)
            restoreDialogState.show()
        }
    }

    val historyDialogState = rememberDialogState()
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.onAutoBackupDirectorySelected(uri)
        }
    }

    SettingsScaffold(
        titleRes = R.string.title_settings_backup,
        onBack = navigator::navigateUp,
        settings = settings,
        initialValue = BackupSettings(),
        // 备份页不参与搜索定位: 不论从哪个入口进入都强制回页首,
        // 避免 Navigation 恢复 LazyListState 或搜索目标导致页内滚动。
        destination = null,
        resetScrollWithoutTarget = true,
        snackbarHostState = snackbarHostState,
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
    ) {
        group(title = R.string.title_settings_backup) {
            preference(
                title = R.string.settings_backup_export,
                leadingIcon = Icons.Outlined.Backup,
                onClick = {
                    includeRuleExport = false
                    saveLauncher.launch(SettingsBackupUtil.getBackupFileName())
                }
            )

            preference(
                title = R.string.settings_backup_export_rules,
                leadingIcon = Icons.Outlined.Shield,
                onClick = {
                    includeRuleExport = true
                    saveLauncher.launch(SettingsBackupUtil.getBackupFileName())
                }
            )

            preference(
                title = R.string.settings_backup_import,
                leadingIcon = Icons.Outlined.CloudDownload,
                onClick = {
                    restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/*"))
                }
            )
        }

        group(title = R.string.settings_group_auto_backup) {
            toggleablePreference(
                property = BackupSettings::autoBackupEnabled,
                title = R.string.settings_auto_backup_enabled,
                leadingIcon = Icons.Outlined.Schedule,
            )

            toggleablePreference(
                property = BackupSettings::autoBackupIncludeRules,
                title = R.string.settings_auto_backup_include_rules,
                leadingIcon = Icons.Outlined.Shield,
                enabled = currentPreference.autoBackupEnabled,
            )

            listPref(
                property = BackupSettings::autoBackupInterval,
                title = R.string.settings_auto_backup_interval,
                leadingIcon = Icons.Outlined.Tune,
                enabled = currentPreference.autoBackupEnabled,
                options = persistentMapOf(
                    AutoBackupInterval.DAILY to R.string.auto_backup_interval_daily,
                    AutoBackupInterval.WEEKLY to R.string.auto_backup_interval_weekly,
                    AutoBackupInterval.MONTHLY to R.string.auto_backup_interval_monthly,
                ),
            )

            listPref(
                property = BackupSettings::autoBackupKeepCount,
                title = R.string.settings_auto_backup_keep_count,
                leadingIcon = Icons.Outlined.History,
                enabled = currentPreference.autoBackupEnabled,
                options = persistentMapOf(
                    3 to R.string.auto_backup_keep_count_3,
                    7 to R.string.auto_backup_keep_count_7,
                    14 to R.string.auto_backup_keep_count_14,
                    30 to R.string.auto_backup_keep_count_30,
                ),
            )

            val directorySummary = currentPreference.autoBackupDirectoryUri?.let {
                DocumentFile.fromTreeUri(context, Uri.parse(it))?.name ?: it
            } ?: context.getString(R.string.settings_auto_backup_private_directory)

            preference(
                onClick = { directoryLauncher.launch(null) },
                title = { Text(text = stringResource(R.string.settings_auto_backup_directory)) },
                summary = { Text(text = directorySummary) },
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                enabled = currentPreference.autoBackupEnabled,
            )

            preference(
                onClick = { viewModel.onAutoBackupDirectorySelected(null) },
                title = { Text(text = stringResource(R.string.settings_auto_backup_use_private_directory)) },
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                enabled = currentPreference.autoBackupEnabled,
            )

            preference(
                onClick = viewModel::onRunBackupNow,
                title = { Text(text = stringResource(R.string.settings_auto_backup_run_now)) },
                icon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
            )

            preference(
                onClick = {
                    viewModel.onLoadAutoBackups()
                    historyDialogState.show()
                },
                title = { Text(text = stringResource(R.string.settings_auto_backup_history)) },
                icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                enabled = currentPreference.autoBackupEnabled,
            )

            val lastBackupAt = currentPreference.lastAutoBackupAt
            val lastBackupText = if (lastBackupAt > 0L) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastBackupAt))
            } else {
                context.getString(R.string.settings_auto_backup_never)
            }
            val lastStatusRes = when (currentPreference.lastAutoBackupSucceeded) {
                true -> R.string.settings_auto_backup_last_success
                false -> R.string.settings_auto_backup_last_failed
                null -> R.string.settings_auto_backup_last_unknown
            }
            preference(
                title = { Text(text = stringResource(R.string.settings_auto_backup_last_run)) },
                summary = {
                    Text(
                        text = lastBackupText + " / " + stringResource(lastStatusRes) +
                                (currentPreference.lastAutoBackupMessage?.let { " / $it" } ?: "")
                    )
                },
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                onClick = {},
            )
        }
    }

    StrongBox {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.pendingRestore == null && uiState.error == null && !uiState.loading) {
            LaunchedEffect(restoreDialogState.show) {
                restoreDialogState.show = false
            }
        }

        if (restoreDialogState.show) {
            SettingsBackupRestoreDialog(
                state = restoreDialogState,
                uiState = uiState,
                onRestoreClicked = viewModel::onRestore,
                onCancelClicked = viewModel::onCancelRestore,
            )
        }

        LaunchedEffect(historyDialogState.show) {
            if (historyDialogState.show) {
                viewModel.onLoadAutoBackups()
            }
        }

        if (historyDialogState.show) {
            AutoBackupHistoryDialog(
                state = historyDialogState,
                uiState = uiState,
                onRestore = { item ->
                    historyDialogState.show = false
                    viewModel.onRestoreFilePicked(item.uri)
                    restoreDialogState.show()
                },
                onCancelClicked = viewModel::onCancelRestore,
            )
        }
    }
}

@Composable
private fun SettingsBackupRestoreDialog(
    state: DialogState,
    uiState: SettingsBackupUiState,
    onRestoreClicked: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onCancelClicked: () -> Unit,
) {
    val restoreSettings = rememberSaveable { mutableStateOf(true) }
    val restoreRules = rememberSaveable { mutableStateOf(true) }
    val restoreForum = rememberSaveable { mutableStateOf(true) }
    val restoreKeyword = rememberSaveable { mutableStateOf(true) }
    val restoreUser = rememberSaveable { mutableStateOf(true) }
    val restoreHidden = rememberSaveable { mutableStateOf(true) }
    val metadata = uiState.pendingRestore?.first

    AlertDialog(
        dialogState = state,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            val titleRes = when {
                uiState.error != null -> R.string.error_tip
                uiState.pendingRestore != null -> R.string.dialog_restore_select_backup
                else -> R.string.dialog_content_wait
            }
            Text(text = stringResource(titleRes))
        },
        buttons = {
            AnimatedVisibility(
                visible = !uiState.loading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DialogNegativeButton(
                        text = stringResource(R.string.button_cancel),
                        onClick = onCancelClicked,
                    )

                    if (uiState.error == null && metadata != null) {
                        val enabled = restoreSettings.value || (metadata.containsBlockRules && restoreRules.value &&
                                (restoreForum.value || restoreKeyword.value || restoreUser.value || restoreHidden.value))
                        Button(
                            onClick = {
                                onRestoreClicked(
                                    restoreSettings.value,
                                    metadata.containsBlockRules && restoreRules.value,
                                    restoreForum.value,
                                    restoreKeyword.value,
                                    restoreUser.value,
                                    restoreHidden.value,
                                )
                            },
                            enabled = enabled,
                            content = { Text(text = stringResource(R.string.button_sure)) },
                        )
                    }
                }
            }
        },
    ) {
        if (uiState.error != null) {
            Text(text = uiState.error.getErrorMessage(), modifier = Modifier.padding(horizontal = 16.dp))
        } else if (uiState.loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                LoadingIndicator()
            }
        } else if (metadata != null) {
            val createdAt = remember(metadata.createdAt) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(metadata.createdAt))
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = stringResource(R.string.settings_backup_created_at, createdAt))
                Text(text = stringResource(R.string.settings_backup_version, metadata.schemaVersion))

                Spacer(modifier = Modifier.height(12.dp))

                BackupRestoreOption(
                    checked = restoreSettings.value,
                    onCheckedChange = { restoreSettings.value = it },
                    title = stringResource(R.string.settings_backup_restore_settings),
                )

                if (metadata.containsBlockRules) {
                    BackupRestoreOption(
                        checked = restoreRules.value,
                        onCheckedChange = { restoreRules.value = it },
                        title = stringResource(R.string.settings_backup_restore_rules),
                    )

                    if (restoreRules.value) {
                        BackupRestoreOption(
                            checked = restoreForum.value,
                            onCheckedChange = { restoreForum.value = it },
                            title = stringResource(R.string.title_restore_forum),
                            count = metadata.forumRuleCount,
                        )
                        BackupRestoreOption(
                            checked = restoreKeyword.value,
                            onCheckedChange = { restoreKeyword.value = it },
                            title = stringResource(R.string.title_restore_keyword),
                            count = metadata.keywordRuleCount,
                        )
                        BackupRestoreOption(
                            checked = restoreUser.value,
                            onCheckedChange = { restoreUser.value = it },
                            title = stringResource(R.string.title_restore_user),
                            count = metadata.userRuleCount,
                        )
                        BackupRestoreOption(
                            checked = restoreHidden.value,
                            onCheckedChange = { restoreHidden.value = it },
                            title = stringResource(R.string.title_restore_hidden),
                            count = metadata.hiddenPostCount,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_backup_empty_rules),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoBackupHistoryDialog(
    state: DialogState,
    uiState: SettingsBackupUiState,
    onRestore: (BackupHistoryItem) -> Unit,
    onCancelClicked: () -> Unit,
) {
    AlertDialog(
        dialogState = state,
        title = { Text(text = stringResource(R.string.settings_auto_backup_history)) },
        buttons = {
            DialogNegativeButton(
                text = stringResource(R.string.button_cancel),
                onClick = onCancelClicked,
            )
        },
    ) {
        if (uiState.loadingAutoBackups) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                LoadingIndicator()
            }
        } else if (uiState.autoBackups.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_auto_backup_no_history),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.autoBackups.forEach { item ->
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(item.lastModified))
                    Text(
                        text = "${item.name}\n$time",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRestore(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    count: Int? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = title, modifier = Modifier.weight(1f))
        count?.let { Text(text = stringResource(R.string.summary_rules_count, it)) }
    }
}
