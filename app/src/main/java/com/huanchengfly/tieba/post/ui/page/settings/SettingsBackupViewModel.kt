package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.HiddenThreadDao
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.SettingsBackupMetadata
import com.huanchengfly.tieba.post.utils.RestoreOption
import com.huanchengfly.tieba.post.utils.SettingsBackupUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Clock

data class SettingsBackupUiState(
    val loading: Boolean = false,
    val pendingRestore: Pair<SettingsBackupMetadata, Uri>? = null,
    val error: Throwable? = null,
)

sealed interface SettingsBackupUiEvent : UiEvent {
    object BadBackup : SettingsBackupUiEvent

    object UnsupportedBackup : SettingsBackupUiEvent

    class BackupFailed(val message: String) : SettingsBackupUiEvent

    object BackupCompleted : SettingsBackupUiEvent

    object RestoreCompleted : SettingsBackupUiEvent
}

private const val TAG = "SettingsBackupViewModel"

@HiltViewModel
class SettingsBackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val blockDao: BlockDao,
    private val hiddenDao: HiddenThreadDao,
    private val transactionRunner: TransactionRunner,
    private val okSignRepository: OKSignRepository,
) : BaseStateViewModel<SettingsBackupUiState>() {

    override fun createInitialState() = SettingsBackupUiState()

    fun onBackup(uri: Uri, timestamp: Long, includeBlockRules: Boolean) {
        if (currentState.loading) return else _uiState.update { it.copy(loading = true) }
        launchInVM {
            runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { output ->
                    SettingsBackupUtil.backup(
                        context = context,
                        settingsRepository = settingsRepository,
                        output = output,
                        timestamp = timestamp,
                        includeBlockRules = includeBlockRules,
                        blockDao = blockDao,
                        hiddenDao = hiddenDao,
                        transactionRunner = transactionRunner,
                    )
                }
            }.onFailure { e ->
                Log.e(TAG, "onBackup", e)
                emitUiEvent(SettingsBackupUiEvent.BackupFailed(e.getErrorMessage()))
                launchInVM(Dispatchers.IO) {
                    runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() }
                }
            }.onSuccess {
                emitUiEvent(SettingsBackupUiEvent.BackupCompleted)
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun onRestoreFilePicked(uri: Uri) {
        if (currentState.loading) return else _uiState.update { it.copy(loading = true) }
        launchInVM(Dispatchers.IO) {
            val result = runCatching {
                val document = DocumentFile.fromSingleUri(context, uri)!!
                require(document.exists() && document.isFile)
                val metadata = context.contentResolver.openInputStream(uri)!!.use { input ->
                    SettingsBackupUtil.readMetadata(input)
                }
                require(metadata.schemaVersion <= SettingsBackupUtil.SCHEMA_VERSION)
                metadata to uri
            }.onFailure { e ->
                Log.w(TAG, "onRestoreFilePicked", e)
                emitUiEvent(
                    if (e is IllegalArgumentException) {
                        SettingsBackupUiEvent.UnsupportedBackup
                    } else {
                        SettingsBackupUiEvent.BadBackup
                    }
                )
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    pendingRestore = result.getOrNull(),
                    error = null,
                )
            }
        }
    }

    fun onRestore(
        includeSettings: Boolean,
        includeBlockRules: Boolean,
        forum: Boolean,
        keyword: Boolean,
        user: Boolean,
        hidden: Boolean,
    ) {
        val state = currentState
        val pending = state.pendingRestore ?: return
        if (state.loading) return

        _uiState.update { it.copy(loading = true) }
        launchInVM {
            val restoreOption = (if (forum) 0 else RestoreOption.EXCLUDE_FORUM) or
                    (if (keyword) 0 else RestoreOption.EXCLUDE_KEYWORD) or
                    (if (user) 0 else RestoreOption.EXCLUDE_USER) or
                    (if (hidden) 0 else RestoreOption.EXCLUDE_HIDDEN)

            runCatching {
                val (_, uri) = pending
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    SettingsBackupUtil.restore(
                        context = context,
                        settingsRepository = settingsRepository,
                        input = input,
                        includeSettings = includeSettings,
                        includeBlockRules = includeBlockRules,
                        blockDao = blockDao,
                        hiddenDao = hiddenDao,
                        transactionRunner = transactionRunner,
                        restoreOption = restoreOption,
                    )
                }
                if (includeSettings) {
                    okSignRepository.scheduleWorker()
                }
            }.onSuccess {
                emitUiEvent(SettingsBackupUiEvent.RestoreCompleted)
                _uiState.update { it.copy(loading = false, pendingRestore = null, error = null) }
            }.onFailure { e ->
                Log.e(TAG, "onRestore", e)
                _uiState.update { it.copy(loading = false, error = e) }
            }
        }
    }

    fun onCancelRestore() = _uiState.update { createInitialState() }
}
