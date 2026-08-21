package com.huanchengfly.tieba.post.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.LocalRealWindowAdaptiveInfo
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.utils.ColorUtils

val LocalExtendedColorScheme = staticCompositionLocalOf { DefaultColors }

val DefaultColors = ExtendedColorScheme(BlueColorScheme.lightColor, darkTheme = false)

val DefaultDarkColors = ExtendedColorScheme(BlueColorScheme.darkColor, darkTheme = true)

@Composable
fun TiebaLiteTheme(
    colorSchemeExt: ExtendedColorScheme = if (isSystemInDarkTheme()) DefaultDarkColors else DefaultColors,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    // 折叠/旋转后窗口容器尺寸可能未及时刷新, 导致 currentWindowAdaptiveInfo()
    // 提供的尺寸类别停留在旧方向. 这里以 LocalConfiguration(配置变化必然更新)
    // 为尺寸真值源, 并叠加 BaseComposeActivity 的 configVersion 强制重算.
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val configVersion = (context.findActivity() as? BaseComposeActivity)?.configVersion ?: 0
    val posture = currentWindowAdaptiveInfo().windowPosture
    val windowSizeClass = remember(configuration, configVersion) {
        WindowSizeClass(configuration.screenWidthDp, configuration.screenHeightDp)
    }
    val windowAdaptiveInfo = remember(windowSizeClass, posture) {
        WindowAdaptiveInfo(windowSizeClass, posture)
    }
    CompositionLocalProvider(
        LocalExtendedColorScheme provides colorSchemeExt,
        LocalWindowAdaptiveInfo provides windowAdaptiveInfo,
        LocalRealWindowAdaptiveInfo provides windowAdaptiveInfo,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorSchemeExt.colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}

val ColorScheme.isTranslucent: Boolean
    get() = surface == Color.Transparent

val ColorScheme.isDarkScheme: Boolean
    get() = ColorUtils.isColorLight(onSurface.toArgb())

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object TiebaLiteTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.colorScheme

    val extendedColorScheme: ExtendedColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current

    val topAppBarColors: TopAppBarColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.appBarColors

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}
