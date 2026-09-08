package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarOverride
import androidx.compose.material3.ShortNavigationBarOverrideScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.roundToInt
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.main.FloatingIconNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.FloatingNavigationBarOverride.ShortNavigationBar
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TallNavigationBarHeight
import com.huanchengfly.tieba.post.utils.LocalAccount

val floatingNavigationBarCompactScreenOffset: Dp
    @Composable
    get() = if (WindowInsets.navigationBars.getBottom(LocalDensity.current) > 0) {
        FloatingToolbarDefaults.ScreenOffset / 4
    } else {
        FloatingToolbarDefaults.ScreenOffset
    }

private val FloatingNavigationBarElevation: Dp = 1.dp

private val FloatingIconNavigationBarHeight = NavigationBarHeight

private val PiliFloatingNavigationItemWidth = 76.dp
private val PiliFloatingNavigationIndicatorShape = RoundedCornerShape(50)
private const val PiliFloatingNavigationMaxTextScale = 1.3f
private const val PiliFloatingNavigationIconLayoutId = "icon"
private const val PiliFloatingNavigationLabelLayoutId = "label"
private val PiliFloatingNavigationIconLabelSpacing = 2.dp

val ColorScheme.vibrantFloatingNavigationBarColor: Color
    @Composable
    get() = if (TiebaLiteTheme.extendedColorScheme.darkTheme) {
        surfaceContainerHighest.copy(alpha = 0.08f).compositeOver(surfaceContainer)
    } else {
        surfaceContainer
    }

val ColorScheme.vibrantFloatingNavigationBarContentColor: Color
    get() = onSurface

/**
 * Drawer primary action used in [NavigationSuite]
 * */
@Composable
fun TbDrawerNavigationAction(
    onLoginClicked: () -> Unit,
    modifier: Modifier = Modifier,
    account: Account? = LocalAccount.current,
) {
    if (account != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(all = 16.dp)
        ) {
            AccountNavIcon(onLoginClicked = onLoginClicked, size = Sizes.Large)
            Text(
                text = account.nickname ?: account.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
    } else {
        val context = LocalContext.current
        Row(
            modifier = modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(data = R.drawable.ic_launcher_new_round, size = Sizes.Small)
            Text(
                text = remember { context.getString(R.string.app_name).uppercase() },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@NonRestartableComposable
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    androidx.compose.material3.NavigationDrawerItem(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        badge = badge,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * Icon only short navigation bar item.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services
 * @param modifier the [Modifier] to be applied to this item
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this item. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this item in different states
 */
@Composable
internal fun IconNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    colors: NavigationItemColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val iconColor = colors.iconColor(selected = selected, enabled = enabled)
    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .size(LocalMinimumInteractiveComponentSize.current),
    ) {
        val indicatorAnimationProgress = animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .badgeBounds(),
            contentAlignment = Alignment.Center,
            content = {
                // Create the indicator ripple.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .indication(interactionSource, ripple())
                )
                // Create the indicator. The indicator has a expansion animation which interferes
                // with the timing of the ripple, which is why they are separate composables.
                Indicator(colors.selectedIndicatorColor) {
                    indicatorAnimationProgress.value.coerceAtLeast(0f)
                }

                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            },
        )
    }
}

@Composable
private fun BoxScope.Indicator(
    indicatorColor: Color,
    indicatorAnimationProgress: () -> Float,
) {
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                drawCircle(indicatorColor, size.minDimension / 2 * indicatorAnimationProgress())
            }
    )
}

context(scope: ShortNavigationBarOverrideScope)
private fun Modifier.floatingNavBarContainer(
    height: Dp,
    screenOffset: Dp,
    isTransitionActive: () -> Boolean,
): Modifier = this then with(scope) {
    Modifier
        .windowInsetsPadding(windowInsets)
        .heightIn(min = height)
        .clickableNoIndication(onClick = {})
        .selectableGroup()
        .graphicsLayer {
            this.shape = CircleShape
            this.clip = true
            this.translationY = -screenOffset.toPx()
            if (!isTransitionActive()) {
                this.shadowElevation = FloatingNavigationBarElevation.toPx()
            }
        }
        .then(modifier)
        .background(color = containerColor, shape = CircleShape)
}

/**
 * This override provides the default behavior of the [ShortNavigationBar] component.
 */
@ExperimentalMaterial3ComponentOverrideApi
object FloatingNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val itemCount = LocalMainNavigationItemCount.current
        val widthFraction = (itemCount.toFloat() / MaxFloatingNavigationItems).coerceIn(0f, 1f)
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .floatingNavBarContainer(
                            height = NavigationBarHeight,
                            screenOffset = floatingNavigationBarCompactScreenOffset,
                            isTransitionActive = { animatedVisibilityScope?.transition?.isRunning == true }
                        )
                        .padding(start = 24.dp, top = 6.dp, end = 24.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * PiliPlus style floating navigation bar with a full-width active indicator behind both icon and
 * label.
 */
@ExperimentalMaterial3ComponentOverrideApi
object PiliFloatingNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Layout(
                    modifier = Modifier
                        .floatingNavBarContainer(
                            height = NavigationBarHeight,
                            screenOffset = floatingNavigationBarCompactScreenOffset,
                            isTransitionActive = {
                                animatedVisibilityScope?.transition?.isRunning == true
                            }
                        )
                        .then(modifier),
                    content = content,
                    measurePolicy = PiliFloatingNavigationBarMeasurePolicy,
                )
            }
        }
    }
}

private object PiliFloatingNavigationBarMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val itemCount = measurables.size
        if (itemCount < 1) {
            return layout(width = 0, height = 0) {}
        }

        val maxItemWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth / itemCount
        } else {
            PiliFloatingNavigationItemWidth.roundToPx()
        }
        val itemWidth = minOf(PiliFloatingNavigationItemWidth.roundToPx(), maxItemWidth)

        val desiredHeight = NavigationBarHeight.roundToPx()
        val itemHeight = if (constraints.hasBoundedHeight) {
            desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        } else {
            desiredHeight.coerceAtLeast(constraints.minHeight)
        }

        val itemPlaceables = measurables.fastMap { measurable ->
            measurable.measure(
                constraints.constrain(
                    Constraints.fixed(width = itemWidth, height = itemHeight)
                )
            )
        }
        return layout(width = itemWidth * itemCount, height = itemHeight) {
            var x = 0
            itemPlaceables.fastForEach { placeable ->
                placeable.placeRelative(x = x, y = 0)
                x += placeable.width
            }
        }
    }
}

@Composable
internal fun PiliFloatingNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    labelVisible: Boolean = true,
    colors: NavigationItemColors,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val indicatorAnimationProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
    )
    val labelAnimationProgress by animateFloatAsState(
        targetValue = if (labelVisible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
    )
    val iconColor = colors.iconColor(selected = selected, enabled = enabled)
    val textColor = colors.textColor(selected = selected, enabled = enabled)
    val density = LocalDensity.current
    val clampedTextDensity = remember(density) {
        Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(PiliFloatingNavigationMaxTextScale)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(PiliFloatingNavigationIndicatorShape)
                .indication(interactionSource, ripple())
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize()
                .padding(vertical = 4.dp)
                .graphicsLayer {
                    scaleX = if (indicatorAnimationProgress == 0f) 0f else {
                        0.5f + 0.5f * indicatorAnimationProgress
                    }
                    alpha = indicatorAnimationProgress
                }
                .clip(PiliFloatingNavigationIndicatorShape)
                .background(color = colors.selectedIndicatorColor),
        )

        Layout(
            content = {
                Box(modifier = Modifier.layoutId(PiliFloatingNavigationIconLayoutId)) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) {
                        icon()
                    }
                }
                if (label != null) {
                    Box(
                        modifier = Modifier
                            .layoutId(PiliFloatingNavigationLabelLayoutId)
                            .graphicsLayer { alpha = labelAnimationProgress },
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides textColor,
                            LocalTextStyle provides MaterialTheme.typography.labelMedium,
                            LocalDensity provides clampedTextDensity,
                        ) {
                            label()
                        }
                    }
                }
            },
            measurePolicy = { measurables, constraints ->
                val iconPlaceable = measurables.fastFirst { measurable ->
                    measurable.layoutId == PiliFloatingNavigationIconLayoutId
                }.measure(constraints.copy(minWidth = 0, minHeight = 0))

                val labelPlaceable = measurables.fastFirstOrNull { measurable ->
                    measurable.layoutId == PiliFloatingNavigationLabelLayoutId
                }?.measure(constraints.copy(minWidth = 0, minHeight = 0))

                val labelHeight = labelPlaceable?.height ?: 0
                val spacing = PiliFloatingNavigationIconLabelSpacing.roundToPx()
                val offsetY = lerp(
                    start = iconPlaceable.height / 2f,
                    stop = iconPlaceable.height / 2f + labelHeight / 2f + spacing,
                    fraction = labelAnimationProgress,
                )
                val iconY = constraints.maxHeight / 2f - offsetY
                val iconX = (constraints.maxWidth - iconPlaceable.width) / 2

                layout(width = constraints.maxWidth, height = constraints.maxHeight) {
                    iconPlaceable.placeRelative(
                        x = iconX,
                        y = iconY.roundToInt(),
                    )
                    if (labelPlaceable != null) {
                        labelPlaceable.placeRelative(
                            x = (constraints.maxWidth - labelPlaceable.width) / 2,
                            y = iconY.roundToInt() + iconPlaceable.height + spacing,
                        )
                    }
                }
            },
        )
    }
}

/**
 * This override provides the default behavior of the [ShortNavigationBar] component.
 */
@ExperimentalMaterial3ComponentOverrideApi
object FloatingIconNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .floatingNavBarContainer(
                        height = FloatingIconNavigationBarHeight,
                        screenOffset = floatingNavigationBarCompactScreenOffset,
                        isTransitionActive = { animatedVisibilityScope?.transition?.isRunning == true }
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@ExperimentalMaterial3ComponentOverrideApi
object DefaultNavigationBarOverride : ShortNavigationBarOverride {
    @Composable
    override fun ShortNavigationBarOverrideScope.ShortNavigationBar() {
        Surface(color = containerColor, contentColor = contentColor, modifier = modifier) {
            Layout(
                modifier =
                    Modifier
                        .windowInsetsPadding(windowInsets)
                        .height(TallNavigationBarHeight)
                        .selectableGroup(),
                content = content,
                measurePolicy = EqualWeightContentMeasurePolicy,
            )
        }
    }
}

// androidx.compose.material3.EqualWeightContentMeasurePolicy
private object EqualWeightContentMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val width = constraints.maxWidth
        var itemHeight = constraints.minHeight
        val itemsCount = measurables.size
        // If there are no items, bar will be empty.
        if (itemsCount < 1) {
            return layout(width, itemHeight) {}
        }

        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables =
                measurables.fastMap {
                    it.measure(constraints.constrain(Constraints.fixedHeight(height = itemHeight)))
                }
        } else {
            val itemWidth = width / itemsCount
            measurables.fastForEach {
                val measurableHeight = it.maxIntrinsicHeight(itemWidth)
                if (itemHeight < measurableHeight) {
                    itemHeight = measurableHeight.coerceAtMost(constraints.maxHeight)
                }
            }

            // Make sure the item containers have the same width and height.
            itemsPlaceables =
                measurables.fastMap {
                    it.measure(
                        constraints.constrain(
                            Constraints.fixed(width = itemWidth, height = itemHeight)
                        )
                    )
                }
        }

        return layout(width, itemHeight) {
            var x = 0
            val y = 0
            itemsPlaceables.fastForEach { item ->
                item.placeRelative(x, y)
                x += item.width
            }
        }
    }
}

private val BadgeTopRuler = HorizontalRuler()
private val BadgeEndRuler = VerticalRuler()

internal fun Modifier.badgeBounds() =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            width = placeable.width,
            height = placeable.height,
            rulers = {
                // use provides instead of provideRelative cause we will place relative
                // in the badge code
                BadgeEndRuler provides coordinates.size.width.toFloat()
                BadgeTopRuler provides 0f
            },
        ) {
            placeable.place(0, 0)
        }
    }
