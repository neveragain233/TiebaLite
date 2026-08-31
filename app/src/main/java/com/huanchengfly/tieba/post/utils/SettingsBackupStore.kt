package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.HiddenThreadDao
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.BackupSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SettingsBackupStore {
    private const val AUTO_BACKUP_PREFIX = "TiebaLite_Auto_"
    private const val AUTO_BACKUP_SUFFIX = ".tbsettings"

    fun getAutoBackupFileName(date: Date = Date()): String {
        val format = SimpleDateFormat("yy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        return "${AUTO_BACKUP_PREFIX}${format.format(date)}$AUTO_BACKUP_SUFFIX"
    }

    fun privateBackupDir(context: Context): File = File(context.filesDir, "settings_backups")

    suspend fun backup(
        context: Context,
        settingsRepository: SettingsRepository,
        settings: BackupSettings,
        blockDao: BlockDao,
        hiddenDao: HiddenThreadDao,
        transactionRunner: TransactionRunner,
        timestamp: Long = System.currentTimeMillis(),
    ): Uri = withContext(Dispatchers.IO) {
        val directoryUri = settings.autoBackupDirectoryUri
        if (directoryUri != null) {
            backupToSafDirectory(
                context = context,
                directoryUri = Uri.parse(directoryUri),
                settingsRepository = settingsRepository,
                settings = settings,
                blockDao = blockDao,
                hiddenDao = hiddenDao,
                transactionRunner = transactionRunner,
                timestamp = timestamp,
            )
        } else {
            backupToPrivateDirectory(
                context = context,
                settingsRepository = settingsRepository,
                settings = settings,
                blockDao = blockDao,
                hiddenDao = hiddenDao,
                transactionRunner = transactionRunner,
                timestamp = timestamp,
            )
        }
    }

    suspend fun listAutoBackups(
        context: Context,
        directoryUri: String?,
    ): List<com.huanchengfly.tieba.post.ui.models.settings.BackupHistoryItem> = withContext(Dispatchers.IO) {
        if (directoryUri != null) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(directoryUri))
            tree?.listFiles()
                ?.filter { it.isFile && it.name?.startsWith(AUTO_BACKUP_PREFIX) == true && it.name?.endsWith(AUTO_BACKUP_SUFFIX) == true }
                ?.map {
                    com.huanchengfly.tieba.post.ui.models.settings.BackupHistoryItem(
                        uri = it.uri,
                        name = it.name.orEmpty(),
                        lastModified = it.lastModified(),
                    )
                }
                ?.sortedByDescending { it.lastModified }
                .orEmpty()
        } else {
            privateBackupDir(context).listFiles { file ->
                file.isFile && file.name.startsWith(AUTO_BACKUP_PREFIX) && file.name.endsWith(AUTO_BACKUP_SUFFIX)
            }
                ?.map {
                    com.huanchengfly.tieba.post.ui.models.settings.BackupHistoryItem(
                        uri = Uri.fromFile(it),
                        name = it.name,
                        lastModified = it.lastModified(),
                    )
                }
                ?.sortedByDescending { it.lastModified }
                .orEmpty()
        }
    }

    suspend fun cleanup(context: Context, directoryUri: String?, keepCount: Int): Unit = withContext(Dispatchers.IO) {
        val keep = keepCount.coerceAtLeast(1)
        if (directoryUri != null) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(directoryUri)) ?: return@withContext
            tree.listFiles()
                .filter { it.isFile && it.name?.startsWith(AUTO_BACKUP_PREFIX) == true && it.name?.endsWith(AUTO_BACKUP_SUFFIX) == true }
                .sortedByDescending { it.lastModified() }
                .drop(keep)
                .forEach { it.delete() }
        } else {
            privateBackupDir(context).listFiles { file ->
                file.isFile && file.name.startsWith(AUTO_BACKUP_PREFIX) && file.name.endsWith(AUTO_BACKUP_SUFFIX)
            }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(keep)
                ?.forEach { it.delete() }
        }
    }

    private suspend fun backupToSafDirectory(
        context: Context,
        directoryUri: Uri,
        settingsRepository: SettingsRepository,
        settings: BackupSettings,
        blockDao: BlockDao,
        hiddenDao: HiddenThreadDao,
        transactionRunner: TransactionRunner,
        timestamp: Long,
    ): Uri {
        val tree = DocumentFile.fromTreeUri(context, directoryUri)
        requireNotNull(tree) { "Invalid backup directory" }
        require(tree.canWrite()) { "Backup directory is not writable" }

        val fileName = getAutoBackupFileName(Date(timestamp))
        tree.findFile(fileName)?.delete()
        val target = requireNotNull(tree.createFile("application/octet-stream", fileName)) {
            "Unable to create backup file"
        }

        try {
            context.contentResolver.openOutputStream(target.uri)?.use { output ->
                SettingsBackupUtil.backup(
                    context = context,
                    settingsRepository = settingsRepository,
                    output = output,
                    timestamp = timestamp,
                    includeBlockRules = settings.autoBackupIncludeRules,
                    blockDao = blockDao,
                    hiddenDao = hiddenDao,
                    transactionRunner = transactionRunner,
                )
            } ?: throw java.io.IOException("Unable to open backup output")
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        return target.uri
    }

    private suspend fun backupToPrivateDirectory(
        context: Context,
        settingsRepository: SettingsRepository,
        settings: BackupSettings,
        blockDao: BlockDao,
        hiddenDao: HiddenThreadDao,
        transactionRunner: TransactionRunner,
        timestamp: Long,
    ): Uri {
        val directory = privateBackupDir(context).apply { mkdirs() }
        val target = File(directory, getAutoBackupFileName(Date(timestamp)))
        val temp = File.createTempFile("auto_backup_", ".tmp", directory)

        try {
            temp.outputStream().use { output ->
                SettingsBackupUtil.backup(
                    context = context,
                    settingsRepository = settingsRepository,
                    output = output,
                    timestamp = timestamp,
                    includeBlockRules = settings.autoBackupIncludeRules,
                    blockDao = blockDao,
                    hiddenDao = hiddenDao,
                    transactionRunner = transactionRunner,
                )
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) throw java.io.IOException("Unable to finalize backup file")
        } catch (e: Throwable) {
            temp.delete()
            target.delete()
            throw e
        }
        return Uri.fromFile(target)
    }
}
