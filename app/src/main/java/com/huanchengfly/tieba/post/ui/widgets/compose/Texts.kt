package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSpFloat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.spToPxFloat
import com.huanchengfly.tieba.post.theme.tokens.ColorSchemeKeyTokens
import com.huanchengfly.tieba.post.theme.tokens.value
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication

@Composable
fun rememberChipInlineContent(
    text: String,
    padding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 4.dp),
    textStyle: TextStyle = LocalTextStyle.current,
    chipTextStyle: TextStyle = LocalTextStyle.current,
    containerColor: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Tertiary,
    color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnTertiary
): InlineTextContent {
    val textMeasurer = rememberTextMeasurer()
    val textSize = remember(text, textStyle) { textMeasurer.measure(text, textStyle).size }
    val textHeightPx = textStyle.fontSize.value.spToPxFloat() -
            padding.calculateTopPadding().value.dpToPxFloat() -
            padding.calculateBottomPadding().value.dpToPxFloat()
    val fontSize = textHeightPx.pxToSpFloat().sp

    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val horizontalPadding = padding.calculateStartPadding(direction) + padding.calculateEndPadding(direction)
    val verticalPadding = padding.calculateTopPadding() + padding.calculateBottomPadding()
    val widthSp = with(density) { (textSize.width.toDp() + horizontalPadding).toSp() }
    val heightSp = with(density) { (textSize.height.toDp() + verticalPadding).toSp() }
    return remember(widthSp, heightSp) { InlineTextContent(
        placeholder = Placeholder(
            width = widthSp,
            height = heightSp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        ),
        children = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.takeIf { it.isNotBlank() && it != "\uFFFD" } ?: text,
                    style = chipTextStyle.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    textAlign = TextAlign.Center,
                    color = color.value,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .fillMaxWidth()
                        .background(containerColor.value, CircleShape)
                        .padding(padding)
                )
            }
        }
    ) }
}

@Composable
fun rememberPhotoInlineContent(
    text: String = stringResource(R.string.btn_view_photos),
    textStyle: TextStyle = LocalTextStyle.current,
    onClick: (index: Int) -> Unit,
): InlineTextContent {
    val textMeasurer = rememberTextMeasurer()
    val textLayout = remember(text, textStyle) {
        textMeasurer.measure(
            text = text,
            style = textStyle,
            overflow = TextOverflow.Visible,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(),
        )
    }
    val textSize = textLayout.size
    val density = LocalDensity.current
    val iconSize = 14.dp
    val spacing = 2.dp
    val widthSp = with(density) { (textSize.width.toDp() + iconSize + spacing + 4.dp).toSp() }
    val heightSp = with(density) { (textSize.height.toDp() + 2.dp).toSp() }

    return remember(widthSp, heightSp, text, onClick) {
        InlineTextContent(
            placeholder = Placeholder(
                width = widthSp,
                height = heightSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            ),
            children = { indexText ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clickableNoIndication {
                                indexText.toIntOrNull()?.let(onClick)
                            }
                            .padding(horizontal = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Photo,
                            contentDescription = null,
                            tint = ColorSchemeKeyTokens.Primary.value,
                            modifier = Modifier.size(iconSize)
                        )
                        Text(
                            text = text,
                            style = textStyle.copy(
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            color = ColorSchemeKeyTokens.Primary.value,
                        )
                    }
                }
            }
        )
    }
}
