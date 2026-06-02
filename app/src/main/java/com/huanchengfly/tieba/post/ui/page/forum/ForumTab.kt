package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

const val TAB_FORUM_LATEST = 0
const val TAB_FORUM_GOOD = 1

private val TabSortTypes: Options<Int> by unsafeLazy {
    persistentMapOf(
        ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
        ForumSortType.BY_SEND to R.string.title_sort_by_send
    )
}

@Composable
fun ForumTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    sortType: Int,
    onSortTypeChanged: (sortType: Int) -> Unit,
    generalTabs: List<FrsTabInfo>,
) {
    val currentPage = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()

    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tabTextStyle = MaterialTheme.typography.labelLarge.copy(
        letterSpacing = 2.sp
    )

    SecondaryScrollableTabRow(
        selectedTabIndex = currentPage,
        indicator = {
            FancyAnimatedIndicatorWithModifier(index = currentPage, scrollable = true)
        },
        divider = {},
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        TabClickMenu(
            selected = currentPage == TAB_FORUM_LATEST,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_LATEST)
                }
            },
            text = {
                Text(text = stringResource(id = R.string.tab_forum_latest), style = tabTextStyle)
            },
            menuContent = {
                ListPickerMenuItems(
                    items = TabSortTypes,
                    picked = sortType,
                    onItemPicked = onSortTypeChanged
                )
            },
            unselectedContentColor = unselectedContentColor
        )

        Tab(
            selected = currentPage == TAB_FORUM_GOOD,
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(TAB_FORUM_GOOD)
                }
            },
            unselectedContentColor = unselectedContentColor
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.tab_forum_good), style = tabTextStyle)
            }
        }

        // TabName, source_id
        val sortIdMap = remember(generalTabs) {
            mutableStateMapOf<String, Int>()
        }
        generalTabs.forEach { tab ->
            val tabIndex = 2 + generalTabs.indexOf(tab)
            if (tab.sort_menu.isNotEmpty()) {
                TabClickMenu(
                    selected = currentPage == tabIndex,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tabIndex)
                        }
                    },
                    text = {
                        Text(
                            text = tab.tabName,
                            style = tabTextStyle
                        )
                    },
                    menuContent = {
                        ListPickerMenuStringLabelItems(
                            items = tab.sort_menu.associate { it.source_id to it.text },
                            picked = sortIdMap[tab.tabName] ?: 0,
                            onItemPicked = { value: Int ->
                                sortIdMap[tab.tabName] = value
                                coroutineScope.launch {
                                    emitGlobalEventSuspend(GeneralTabListUiEvent.Refresh(sortType = value))
                                }
                            }
                        )
                    },
                    unselectedContentColor = unselectedContentColor
                )
            } else {
                Tab(
                    selected = currentPage == tabIndex,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tabIndex)
                        }
                    },
                    unselectedContentColor = unselectedContentColor
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.tabName,
                            style = tabTextStyle
                        )
                    }
                }
            }
        }
    }
}