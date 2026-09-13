package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.activity.compose.BackHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import androidx.compose.material3.Text

@Composable
fun SettingsSearchPage(navigator: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    var keyword by rememberSaveable { mutableStateOf("") }
    val isKeyboardOpen by rememberUpdatedState(
        WindowInsets.ime.getBottom(density) > 0
    )

    BackHandler(enabled = isKeyboardOpen) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val query = keyword.trim()
    val configuration = LocalConfiguration.current
    val searchIndex by produceState<List<SettingsSearchIndexedEntry>?>(null, context, configuration) {
        value = null
        value = SettingsSearchIndex.index(context)
    }
    var searchResult by remember(query, searchIndex) { mutableStateOf<List<SettingsSearchEntry>?>(null) }
    LaunchedEffect(query, searchIndex) {
        val index = searchIndex ?: return@LaunchedEffect
        searchResult = withContext(Dispatchers.Default) { SettingsSearchIndex.search(query, index) }
    }
    val result = searchResult

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        // Navigation marks the entry RESUMED after its enter transition finishes.
        lifecycleOwner.lifecycle.withResumed { focusRequester.requestFocus() }
    }

    MyScaffold(
        topBar = {
            TopAppBar(
                title = {
                    SettingsSearchField(
                        keyword = keyword,
                        onKeywordChange = { keyword = it },
                        modifier = Modifier.focusRequester(focusRequester),
                        onSearch = { keyboardController?.hide() },
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    if (keyword.isNotEmpty()) {
                        ActionItem(
                            icon = Icons.Rounded.Clear,
                            contentDescription = R.string.button_clear,
                        ) {
                            keyword = ""
                            focusRequester.requestFocus()
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            query.isEmpty() -> {
                SearchStatusScreen(
                    modifier = Modifier.padding(padding),
                    title = stringResource(R.string.tip_search_settings),
                )
            }

            result == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            result.isEmpty() -> {
                SearchStatusScreen(
                    modifier = Modifier.padding(padding),
                    title = stringResource(R.string.tip_search_settings_no_result),
                    showIllustration = true,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    settingsSearchResultsList(
                        result = result,
                        onOpenResult = { entry ->
                            SettingsSearchTarget.set(entry.destination, entry.itemKey)
                            navigator.navigateDebounced(entry.destination)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchStatusScreen(
    title: String,
    modifier: Modifier = Modifier,
    showIllustration: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (showIllustration) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
            )
            TipScreen(
                title = { Text(text = title) },
                scrollable = false,
                image = {
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        } else {
            Text(text = title)
        }
    }
}
