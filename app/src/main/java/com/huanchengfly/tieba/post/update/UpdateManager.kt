package com.huanchengfly.tieba.post.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.utils.DownloadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val REPO = "neveragain233/TiebaLite"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$REPO/releases/latest"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String?,
        val apkUrl: String,
        val apkSize: Long,
    )

    sealed interface DownloadResult {
        data class Success(val file: File) : DownloadResult
        data class Failure(val message: String) : DownloadResult
    }

    /**
     * 检查 GitHub Releases 最新版本。
     * Release tag 需使用 versionCode 命名（如 v391034）；
     * 无 Release、无 APK 附件或版本不高于当前安装版本时返回 null。
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "TiebaLite")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val json = response.body.string()
            val release = JSONObject(json)
            val versionCode = release.optString("tag_name")
                .trimStart('v', 'V')
                .toIntOrNull()
                ?: return@withContext null
            if (versionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val assets = release.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
            val url = apkUrl ?: return@withContext null

            UpdateInfo(
                versionCode = versionCode,
                versionName = release.optString("name").ifBlank { release.optString("tag_name") },
                changelog = release.optString("body").takeIf { it.isNotBlank() },
                apkUrl = url,
                apkSize = apkSize,
            )
        }
    }

    /**
     * 使用系统 DownloadManager 下载 APK 到应用专属目录并跟踪进度。
     * 协程被取消时会移除未完成的下载任务。
     */
    suspend fun downloadAndTrack(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val destinationDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "apk",
        ).apply { mkdirs() }
        val targetFile = File(destinationDir, "TiebaLite-${info.versionName}.apk")
        targetFile.delete()

        val downloadId = try {
            DownloadUtil.enqueueNewDownloadOrThrow(
                uri = Uri.parse(info.apkUrl),
                title = "TiebaLite ${info.versionName}",
                destination = Uri.fromFile(targetFile),
            ) {
                setMimeType(APK_MIME_TYPE)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setAllowedOverMetered(true)
            }
        } catch (e: Exception) {
            return@withContext DownloadResult.Failure(e.message ?: "无法开始下载")
        }

        return@withContext try {
            trackDownload(downloadId, targetFile, info.apkSize, onProgress)
        } finally {
            if (!coroutineContext.isActive) {
                DownloadUtil.downloadManager.remove(downloadId)
            }
        }
    }

    private suspend fun trackDownload(
        downloadId: Long,
        targetFile: File,
        expectedSize: Long,
        onProgress: (Float) -> Unit,
    ): DownloadResult {
        while (true) {
            currentCoroutineContext().ensureActive()
            val progress = queryDownload(downloadId)
            if (progress == null) return DownloadResult.Failure("无法获取下载状态")
            when (progress.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    // 校验下载文件完整性，避免损坏的 APK 导致安装时解析失败
                    if (expectedSize > 0L && targetFile.length() != expectedSize) {
                        return DownloadResult.Failure("下载文件校验失败，请重试")
                    }
                    return DownloadResult.Success(targetFile)
                }

                DownloadManager.STATUS_FAILED ->
                    return DownloadResult.Failure("下载失败（错误码 ${progress.reason}）")

                else -> {
                    if (progress.total > 0) {
                        val fraction =
                            (progress.downloaded.toFloat() / progress.total).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main.immediate) { onProgress(fraction) }
                    }
                    delay(500)
                }
            }
        }
    }

    fun installApk(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.share.FileProvider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private data class DownloadProgress(
        val status: Int,
        val downloaded: Long,
        val total: Long,
        val reason: Int,
    )

    private fun queryDownload(downloadId: Long): DownloadProgress? {
        return try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = DownloadUtil.downloadManager.query(query)
            cursor?.use {
                if (it.moveToFirst()) {
                    DownloadProgress(
                        status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                        downloaded = it.getLong(
                            it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        ),
                        total = it.getLong(
                            it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        ),
                        reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
