package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference

/**
 * 参考截图样式的设置页顶部搜索条: 圆角浅色底 + 左侧放大镜 + 输入占位.
 */
@Composable
fun SettingsSearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (keyword.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.title_search),
                                    color = colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}

/**
 * 设置搜索匹配结果(按子页分组), 追加到主控 LazyColumn.
 */
fun LazyListScope.settingsSearchResultsList(
    result: List<SettingsSearchEntry>,
    onOpenResult: (SettingsSearchEntry) -> Unit,
) {
    if (result.isEmpty()) {
        item(key = "search_empty") {
            Text(
                text = stringResource(R.string.tip_search_settings_no_result),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        result.groupBy { it.destination }.forEach { group ->
            item(key = "header_${group.key}") {
                Text(
                    text = stringResource(destinationLabelRes(group.key)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            group.value.forEach { entry ->
                item(key = entry.titleRes) {
                    val summaryText = if (entry.summaryRes != null) {
                        stringResource(entry.summaryRes)
                    } else {
                        null
                    }
                    SegmentedPreference(
                        title = stringResource(entry.titleRes),
                        summary = summaryText,
                        shapes = ListItemDefaults.shapes().run { copy(shape = selectedShape) },
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
