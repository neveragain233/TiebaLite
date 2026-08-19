package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FabPosition
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.LocalRealWindowAdaptiveInfo

/**
 * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
 * ergonomics and reachability depending upon the height of the device.
 */
fun calculateNavigationPosition(adaptiveInfo: WindowAdaptiveInfo): Arrangement.Vertical = with(adaptiveInfo) {
    when {
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
            Arrangement.Center
        }

        else -> NavigationSuiteDefaults.verticalArrangement
    }
}

fun calculateNavigationType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType = with(adaptiveInfo) {
    when {
        windowPosture.isTabletop -> NavigationSuiteType.ShortNavigationBarMedium

        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationRail
        }

        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationDrawer
        }

        // 内屏(medium, >=600dp)起使用左侧 Rail, 折叠屏展开后主导航常驻侧栏
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            NavigationSuiteType.NavigationRail
        }

        else -> NavigationSuiteType.NavigationBar
    }
}

/**
 * 回顶 FAB 的位置: 内屏(>=600dp)放在左下贴近常驻侧栏, 手机保持右下.
 */
@Composable
@ReadOnlyComposable
fun backToTopFabPosition(): FabPosition {
    val adaptiveInfo = LocalRealWindowAdaptiveInfo.current
    return if (adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        FabPosition.Start
    } else {
        FabPosition.End
    }
}
