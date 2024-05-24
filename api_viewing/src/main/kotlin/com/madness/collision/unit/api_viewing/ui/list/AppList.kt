/*
 * Copyright 2024 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.Democratic
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.unit.api_viewing.ComposeUnit
import com.madness.collision.util.FilePop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class AppListFragment : ComposeUnit(), Democratic {
    override val id: String = "AV"

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
//        mainViewModel.configNavigation(toolbar, iconColor)
//        toolbar.setTitle(com.madness.collision.R.string.apiViewer)
        toolbar.isVisible = false
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        democratize(mainViewModel)
        val viewModel by viewModels<AppListViewModel>()
        viewModel.events
            .filterIsInstance<AppListEvent.ShareAppList>()
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach ev@{ event ->
                val context = context ?: return@ev
                val title = com.madness.collision.R.string.fileActionsShare
                FilePop.by(context, event.file, "text/csv", title, imageLabel = "App List")
                    .show(childFragmentManager, FilePop.TAG)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppList(paddingValues = rememberContentPadding())
        }
    }
}

@Composable
fun AppList(paddingValues: PaddingValues) {
    val viewModel = viewModel<AppListViewModel>()
    val appList by viewModel.appList.collectAsState()
    val opUiState by viewModel.opUiState.collectAsState()
    AppListScaffold(
        listState = rememberAppListState(viewModel),
        eventHandler = rememberCompOptionsEventHandler(viewModel),
        paddingValues = paddingValues
    ) { loadedCats, contentPadding ->
        LegacyAppList(
            appList = appList,
            options = opUiState.options,
            loadedCats = loadedCats,
            headerState = rememberListHeaderState(ListSrcCat.Platform, viewModel),
            paddingValues = contentPadding,
        )
    }
}

@Composable
private fun AppListV2(paddingValues: PaddingValues) {
    val viewModel = viewModel<AppListViewModel>()
    val artList by viewModel.uiState.collectAsState()
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))) {
        LazyColumn(contentPadding = paddingValues) {
            items(items = artList, key = { p -> p.packageName }) { art ->
                AppListItem(art = art)
            }
        }
    }
}

@Stable
interface AppListState {
    val isRefreshing: Boolean
    val loadedCats: Set<ListSrcCat>
    val opUiState: AppListOpUiState
}

@Composable
private fun AppListScaffold(
    listState: AppListState,
    eventHandler: CompositeOptionsEventHandler,
    paddingValues: PaddingValues,
    content: @Composable (Set<ListSrcCat>, PaddingValues) -> Unit
) {
    var showListOptions by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            AppListBar(
                isRefreshing = listState.isRefreshing,
                windowInsets = WindowInsets(top = 28.dp)
            ) {
                AppListBarAction(
                    icon = Icons.Outlined.CheckCircle,
                    label = listState.opUiState.options.srcSet.size.takeIf { it > 0 }?.toString(),
                    onClick = { showListOptions++ }
                )
            }
        },
        content = { contentPadding ->
            Box() {
                content(listState.loadedCats, contentPadding)
                ListOptionsDialog(
                    isShown = showListOptions,
                    options = listState.opUiState.options,
                    eventHandler = eventHandler
                )
            }
        }
    )
}