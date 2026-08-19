package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.LocalAccount

/** 应用级常驻侧栏的宽度, 同时用于内容区的左侧留白. */
internal val AppLevelRailWidth = 80.dp

/**
 * 应用级常驻侧栏: 非紧凑窗口下跨所有根目的地显示, 点击可直接回到根 tab.
 */
@Composable
fun AppLevelNavigationRail(
    modifier: Modifier = Modifier,
    onSelect: (MainDestination) -> Unit,
    onLoginClick: () -> Unit,
) {
    val uiSettings = LocalUISettings.current
    val loggedIn = LocalAccount.current != null
    val mainNavState = LocalMainNavState.current
    val destinations = listOfNotNull(
        MainDestination.Home,
        MainDestination.Explore.takeUnless { uiSettings.hideExplore },
        MainDestination.Notification.takeIf { loggedIn },
        MainDestination.User,
    )

    NavigationRail(modifier = modifier.fillMaxHeight()) {
        // 顶部占位使图标组垂直居中, 与根页面 Rail 一致
        Spacer(modifier = Modifier.weight(1f))
        destinations.forEach { destination ->
            val selected = destination === mainNavState.currentTab
            NavigationRailItem(
                selected = selected,
                onClick = {
                    mainNavState.requestedTab = destination
                    onSelect(destination)
                },
                icon = {
                    Icon(
                        modifier = Modifier.size(Sizes.Tiny),
                        painter = rememberAnimatedVectorPainter(
                            animatedImageVector = AnimatedImageVector.animatedVectorResource(destination.iconRes),
                            atEnd = selected,
                        ),
                        contentDescription = stringResource(destination.titleRes),
                    )
                },
                label = { Text(stringResource(destination.titleRes)) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        AccountNavIcon(onLoginClicked = onLoginClick)
    }
}
