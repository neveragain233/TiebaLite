package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.icons.GitHubInvertocat
import com.huanchengfly.tieba.post.ui.icons.License
import com.huanchengfly.tieba.post.ui.page.welcome.UaWebView
import com.huanchengfly.tieba.post.update.UpdateManager
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val URL_PROJECT_GITHUB = "https://github.com/0ranko0P/TiebaLite"
private const val URL_PROJECT_FORK_GITHUB = "https://github.com/neveragain233/TiebaLite"

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val info: UpdateManager.UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Float) : UpdateUiState
    data class Downloaded(val info: UpdateManager.UpdateInfo, val file: File) : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

private fun formatApkSize(size: Long): String? {
    if (size <= 0) return null
    return when {
        size >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", size / 1024f / 1024f)
        size >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", size / 1024f)
        else -> "$size B"
    }
}

@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onDisclaimerClicked: () -> Unit = {},
    onHomePageClicked: () -> Unit = {},
    onUpstreamClicked: () -> Unit = {},
    onLicenseClicked: () -> Unit = {},
    onCheckUpdateClicked: () -> Unit = {},
    checkUpdateEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val windowSizeClass = LocalWindowAdaptiveInfo.current.windowSizeClass
    val isWindowHeightExpanded = windowSizeClass.isHeightAtLeastBreakpoint(
        heightDpBreakpoint = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
    )
    val icons = remember {
        listOf(
            R.mipmap.ic_launcher_new_round,
            R.mipmap.ic_launcher_new_invert_round,
            R.mipmap.ic_launcher_round,
        )
    }

    val buildTime = remember {
        val buildDate = Date(BuildConfig.BUILD_TIME * 1000)
        // DateTimeFormatter#ISO_INSTANT
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(buildDate)
    }

    SettingsScaffold(
        modifier = modifier,
        titleRes = R.string.title_about,
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        onBack = onBackClicked,
    ) {
        customPreference {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isWindowHeightExpanded) {
                    Spacer(modifier = Modifier.height(48.dp))
                } else {
                    Spacer(modifier = Modifier.height(36.dp))
                }

                StrongBox {
                    var iconIndex by rememberSaveable { mutableIntStateOf(0) }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(icons[iconIndex])
                            .crossfade(false) // Coil alpha bug in API 28
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clickableNoIndication {
                                iconIndex = (iconIndex + 1).takeIf { it in icons.indices } ?: 0 // Loop icons
                            }
                    )
                }

                Image(
                    painter = painterResource(R.drawable.ic_splash_text),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp, 96.dp)
                        .offset(y = (-8).dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Text(
                    text = stringResource(R.string.welcome_intro_subtitle),
                    modifier = Modifier.offset(y = (-24).dp),
                    style = MaterialTheme.typography.titleMedium
                )

                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Column(
                        modifier = Modifier.offset(y = -(20).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        Text(text = "${BuildConfig.BUILD_TYPE.uppercase()}#${BuildConfig.BUILD_GIT}")
                        Text(text = buildTime)
                    }
                }
            }
        }

        customPreference {
            if (isWindowHeightExpanded){
                Spacer(modifier = Modifier.height(96.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        group {
            preference(
                title = context.getString(R.string.settings_check_update),
                summary = context.getString(R.string.summary_check_update, BuildConfig.VERSION_NAME),
                icon = Icons.Rounded.SystemUpdate,
                enabled = checkUpdateEnabled,
                onClick = onCheckUpdateClicked,
            )

            preference(
                title = context.getString(R.string.title_disclaimer),
                icon = Icons.Rounded.Info,
                onClick = onDisclaimerClicked
            )

            preference(
                title = context.getString(R.string.about_source_code),
                summary = URL_PROJECT_FORK_GITHUB,
                icon = GitHubInvertocat,
                onClick = onHomePageClicked,
            )

            preference(
                title = context.getString(R.string.about_upstream),
                summary = URL_PROJECT_GITHUB,
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                onClick = onUpstreamClicked,
            )

            preference(
                title = context.getString(R.string.about_license),
                summary = "GNU GENERAL PUBLIC LICENSE Version 3",
                icon = Icons.Rounded.License,
                onClick = onLicenseClicked,
            )
        }
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val disclaimerDialogState = rememberDialogState()
    val coroutineScope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun launchCustomTab(url: String) {
        TiebaWebView.launchCustomTab(context, Uri.parse(url))
    }

    fun checkForUpdate() {
        if (updateState == UpdateUiState.Checking || updateState is UpdateUiState.Downloading) return
        updateState = UpdateUiState.Checking
        coroutineScope.launch {
            updateState = try {
                val info = UpdateManager.checkForUpdate()
                if (info == null) {
                    UpdateUiState.UpToDate
                } else {
                    UpdateUiState.Available(info)
                }
            } catch (e: Exception) {
                UpdateUiState.Failed(
                    context.getString(R.string.update_check_failed, e.message ?: "网络错误")
                )
            }
        }
    }

    fun startDownload(info: UpdateManager.UpdateInfo) {
        downloadJob = coroutineScope.launch {
            updateState = UpdateUiState.Downloading(0f)
            updateState = when (val result = UpdateManager.downloadAndTrack(context, info) { progress ->
                updateState = UpdateUiState.Downloading(progress)
            }) {
                is UpdateManager.DownloadResult.Success -> UpdateUiState.Downloaded(info, result.file)
                is UpdateManager.DownloadResult.Failure -> UpdateUiState.Failed(result.message)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        updateState = UpdateUiState.Idle
    }

    fun installUpdate(file: File) {
        val needUnknownSourcesPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
        if (needUnknownSourcesPermission) {
            Toast.makeText(context, R.string.update_install_permission_needed, Toast.LENGTH_LONG).show()
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        if (!UpdateManager.installApk(context, file)) {
            Toast.makeText(context, R.string.update_install_failed, Toast.LENGTH_LONG).show()
        }
        updateState = UpdateUiState.Idle
    }

    AboutPage(
        onBackClicked = onBack,
        onDisclaimerClicked = disclaimerDialogState::show,
        onHomePageClicked = { launchCustomTab(URL_PROJECT_FORK_GITHUB) },
        onUpstreamClicked = { launchCustomTab(URL_PROJECT_GITHUB) },
        onLicenseClicked = { launchCustomTab("${URL_PROJECT_GITHUB}/blob/main/LICENSE") },
        onCheckUpdateClicked = ::checkForUpdate,
        checkUpdateEnabled = updateState != UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
    )

    AlertDialog(
        dialogState = disclaimerDialogState,
        buttons = {
            NegativeButton(text = stringResource(R.string.btn_close)) {
                disclaimerDialogState.show = false
            }
        }
    ) {
        UaWebView(modifier = Modifier.height(480.dp))
    }

    when (val state = updateState) {
        UpdateUiState.Checking -> MaterialAlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.update_checking)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
        )

        is UpdateUiState.Available -> MaterialAlertDialog(
            onDismissRequest = { updateState = UpdateUiState.Idle },
            title = { Text(stringResource(R.string.update_available_title, state.info.versionName)) },
            text = {
                Column {
                    state.info.changelog?.let { Text(it) }
                    formatApkSize(state.info.apkSize)?.let { size ->
                        Text(stringResource(R.string.update_size, size))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { startDownload(state.info) }) {
                    Text(stringResource(R.string.btn_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateState = UpdateUiState.Idle }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        )

        is UpdateUiState.Downloading -> MaterialAlertDialog(
            onDismissRequest = ::cancelDownload,
            title = { Text(stringResource(R.string.update_downloading)) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(state.progress * 100).roundToInt()}%")
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = ::cancelDownload) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        )

        is UpdateUiState.Downloaded -> MaterialAlertDialog(
            onDismissRequest = { updateState = UpdateUiState.Idle },
            title = { Text(stringResource(R.string.update_download_finished)) },
            text = { Text(stringResource(R.string.update_install_prompt, state.info.versionName)) },
            confirmButton = {
                TextButton(onClick = { installUpdate(state.file) }) {
                    Text(stringResource(R.string.update_install))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateState = UpdateUiState.Idle }) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )

        UpdateUiState.UpToDate -> LaunchedEffect(updateState) {
            Toast.makeText(context, R.string.update_up_to_date, Toast.LENGTH_SHORT).show()
            updateState = UpdateUiState.Idle
        }

        is UpdateUiState.Failed -> LaunchedEffect(updateState) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            updateState = UpdateUiState.Idle
        }

        UpdateUiState.Idle -> Unit
    }
}

@Preview("AboutPage", showBackground = true, backgroundColor = -1L)
@Composable
private fun AboutPagePreview() = TiebaLiteTheme {
    AboutPage()
}
