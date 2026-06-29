package com.huanchengfly.tieba.post.utils

import android.app.DownloadManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import java.io.File

object DownloadUtil {

    val downloadManager: DownloadManager by lazy {
        App.INSTANCE.getSystemService(DownloadManager::class.java)!!
    }

    val DEFAULT_DOWNLOAD_DIR: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FileUtil.FILE_FOLDER)

    fun enqueueNewDownloadOrThrow(
        uri: Uri,
        title: String,
        destination: Uri,
        builder: (DownloadManager.Request.() -> Unit)? = null,
    ): Long {
        val request = DownloadManager.Request(uri)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setTitle(title)
            .setDestinationUri(destination)
            .apply {
                builder?.invoke(this)
            }
        return downloadManager.enqueue(request)
    }

    fun downloadVideo(video: Uri, destination: File = DEFAULT_DOWNLOAD_DIR): Long {
        val title = destination.nameWithoutExtension
        return enqueueNewDownloadOrThrow(video, title, Uri.fromFile(destination)) {
            setMimeType(MimeTypes.VIDEO_MP4)
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) allowScanningByMediaScanner()
        }
    }

    /**
     * Query the status of the download, as one of the STATUS_* constants.
     */
    suspend fun queryStatus(query: DownloadManager.Query): Int? = withContext(Dispatchers.IO) {
        var cursor: Cursor? = null
        try {
            cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            } else {
                null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            cursor?.closeQuietly()
        }
    }

    suspend fun queryById(downloadId: Long): Int? {
        return queryStatus(DownloadManager.Query().setFilterById(downloadId))
    }
}