package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference

/**
 * 设置主页的搜索入口: 仅负责展示和跳转, 不承担输入状态.
 */
@Composable
fun SettingsSearchEntryBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.title_search),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * 设置搜索页顶栏输入框: 由父级持有 [focusRequester] 并在页面进入后请求焦点.
 */
@Composable
fun SettingsSearchField(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    BasicTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (keyword.isEmpty()) {
                    Text(
                        text = stringResource(R.string.title_search),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                innerTextField()
            }
        },
    )
}

/**
 * 设置搜索匹配结果(按子页分组), 追加到搜索页 LazyColumn.
 */
fun LazyListScope.settingsSearchResultsList(
    result: List<SettingsSearchEntry>,
    onOpenResult: (SettingsSearchEntry) -> Unit,
) {
    result.groupBy { it.destination }.forEach { (destination, entries) ->
        item(key = "header_$destination") {
            Text(
                text = stringResource(destinationLabelRes(destination)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        entries.forEachIndexed { index, entry ->
            item(key = entry.itemKey ?: entry.titleRes) {
                val summaryText = entry.summaryRes?.let { stringResource(it) }
                SegmentedPreference(
                    title = stringResource(entry.titleRes),
                    summary = summaryText,
                    shapes = segmentedResultShapes(index, entries.size),
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    onClick = { onOpenResult(entry) },
                )
            }
        }
    }
}

@Composable
private fun destinationLabelRes(destination: SettingsDestination): Int = when (destination) {
    SettingsDestination.AccountManage -> R.string.title_account_manage
    SettingsDestination.OKSign -> R.string.title_oksign
    SettingsDestination.BlockSettings -> R.string.title_block_settings
    SettingsDestination.UI -> R.string.title_settings_custom
    SettingsDestination.Habit -> R.string.title_settings_read_habit
    SettingsDestination.Privacy -> R.string.title_settings_privacy
    SettingsDestination.More -> R.string.title_settings_more
    SettingsDestination.Backup -> R.string.title_settings_backup
    SettingsDestination.About -> R.string.title_about
    SettingsDestination.Settings -> R.string.title_settings
    SettingsDestination.WorkInfo -> R.string.title_settings_worker
    SettingsDestination.StickyHeader -> R.string.title_settings_sticky_header
    else -> R.string.title_settings
}

@Composable
private fun segmentedResultShapes(index: Int, count: Int) = if (count == 1) {
    ListItemDefaults.shapes().run { copy(shape = selectedShape) }
} else {
    ListItemDefaults.segmentedShapes(index = index, count = count)
}
