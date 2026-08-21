package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.HiddenThread
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DeleteIconButton
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils

@Composable
fun HiddenPostListPage(
    onBack: () -> Unit,
    onOpenThread: (HiddenThread) -> Unit,
    viewModel: HiddenPostListViewModel = hiltViewModel(),
) {
    val hiddenList by viewModel.blackList.collectAsStateWithLifecycle()
    val isUpdating by viewModel.updating.collectAsStateWithLifecycle()

    val selectedItems = remember { mutableStateSetOf<HiddenThread>() }
    var selectMode by remember { mutableStateOf(false) }

    fun exitSelectMode() {
        selectMode = false
        selectedItems.clear()
    }

    BackHandler(enabled = selectMode) {
        exitSelectMode()
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_hidden_thread_list,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                actions = {
                    FadedVisibility(visible = selectMode || isUpdating) {
                        DeleteIconButton(
                            deleting = isUpdating,
                            enabled = selectedItems.isNotEmpty(),
                        ) {
                            viewModel.delete(selectedItems.toList())
                            exitSelectMode()
                        }
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            ) {}
        },
    ) { contentPadding ->
        val items = hiddenList
        StateScreen(
            isEmpty = items.isNullOrEmpty(),
            isError = false,
            isLoading = items == null,
            screenPadding = contentPadding,
        ) {
            if (items == null) return@StateScreen
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(items, key = { _, it -> it.tid }) { _, item ->
                    val selected = selectedItems.contains(item)
                    HiddenThreadRow(
                        hidden = item,
                        selected = selected,
                        selectMode = selectMode,
                        onClick = {
                            if (selectMode) {
                                if (selected) selectedItems -= item else selectedItems += item
                            } else {
                                onOpenThread(item)
                            }
                        },
                        onLongClick = {
                            if (!isUpdating && !selectMode) {
                                selectedItems += item
                                selectMode = true
                            }
                        },
                        onUnhide = { viewModel.delete(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenThreadRow(
    hidden: HiddenThread,
    selected: Boolean,
    selectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUnhide: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectMode) {
            Checkbox(checked = selected, onCheckedChange = null)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hidden.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = remember(hidden.hiddenTime) {
                    val time = DateTimeUtils.getRelativeTimeString(context, hidden.hiddenTime)
                    "${hidden.forumName} · $time"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!selectMode) {
            TextButton(onClick = onUnhide) {
                Text(text = stringResource(R.string.title_unhide_thread))
            }
        }
    }
}
