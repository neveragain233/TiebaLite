package com.huanchengfly.tieba.post.components.dialogs

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LifecycleOwner
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogMargin
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogAdaptiveFraction
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.applyNavigationBarAppearance
import kotlinx.coroutines.launch

class LoadingDialog(context: Context, @StyleRes themeResId: Int): AlertDialog(context, themeResId) {

    constructor(context: Context): this(context, R.style.Dialog_RequestPermissionTip)

    init {
        setView(ComposeView(context).apply {
            setContent {
                TiebaLiteTheme(ThemeUtil.colorState.value) {
                    Box(
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingDialogContent(
                            modifier = Modifier
                                .fillMaxWidth(fraction = dialogAdaptiveFraction)
                                .heightIn(min = 144.dp)
                                .padding(DefaultDialogMargin),
                        )
                    }
                }
            }
        })
        setCancelable(false)
    }

    override fun onStart() {
        super.onStart()
        val lifecycleOwner = context.findActivity() as? LifecycleOwner ?: return
        lifecycleOwner.lifecycleScope.launch {
            val uiSettings = App.INSTANCE.settingRepository.uiSettings.snapshot()
            window?.let { dialogWindow ->
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.applyNavigationBarAppearance(
                    controller = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView),
                    colorScheme = ThemeUtil.currentColorScheme(),
                    immersiveWhiteNavigationBar = uiSettings.immersiveWhiteNavigationBar,
                )
            }
        }
    }
}

@Composable
fun LoadingDialogContent(modifier: Modifier = Modifier, @StringRes tipText: Int = R.string.text_loading) {
    Surface(
        shape = AlertDialogDefaults.shape,
        tonalElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingIndicator(modifier = Modifier.padding(start = 32.dp))

            Text(
                text = stringResource(tipText),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
