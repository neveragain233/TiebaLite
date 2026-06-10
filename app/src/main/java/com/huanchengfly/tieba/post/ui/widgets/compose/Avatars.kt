package com.huanchengfly.tieba.post.ui.widgets.compose

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@NonRestartableComposable
@Composable
fun AvatarPlaceholder(size: Dp, modifier: Modifier = Modifier)  {
    Box(
        modifier = modifier
            .size(size)
            .placeholder(shape = CircleShape)
    )
}

@Composable
@NonRestartableComposable
fun Avatar(
    data: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = CircleShape,
) {
    if (!data.isNullOrEmpty()) {
        Avatar(data = data, modifier = modifier.size(size), contentDescription, shape)
    } else {
        Avatar(data = DefaultErrorResource, size, modifier, contentDescription, shape)
    }
}

@Composable
@NonRestartableComposable
fun Avatar(
    data: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = CircleShape
) {
    Box(modifier = modifier.clip(shape)) {
        AsyncImage(
            model = data,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            error = painterResource(DefaultErrorResource),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
@NonRestartableComposable
fun Avatar(
    @DrawableRes data: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = CircleShape
) = Image(
    painter = painterResource(id = data),
    contentDescription = contentDescription,
    modifier = modifier
        .size(size)
        .clip(shape = shape),
    contentScale = ContentScale.Crop
)

@Composable
@NonRestartableComposable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}