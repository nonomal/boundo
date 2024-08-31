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

package com.madness.collision.unit.api_viewing.ui.home

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.List
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import com.madness.collision.chief.app.ComposeFragment
import com.madness.collision.chief.app.rememberColorScheme
import com.madness.collision.util.dev.PreviewCombinedColorLayout

class AppHomeFragment : ComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.insetTop.observe(viewLifecycleOwner) { insetTop ->
            mainViewModel.contentWidthTop.value = insetTop
        }
        mainViewModel.insetBottom.observe(viewLifecycleOwner) { insetBottom ->
            mainViewModel.contentWidthBottom.value = insetBottom
        }
    }

    @Composable
    override fun ComposeContent() {
        MaterialTheme(colorScheme = rememberColorScheme()) {
            AppHomePage(paddingValues = rememberContentPadding())
        }
    }
}

interface AppHomeNav {
    fun setNavPage(index: Int)
    fun navBack()
}

@Composable
fun AppHomePage(paddingValues: PaddingValues) {
    var selNavIndex by remember { mutableIntStateOf(0) }
    var homeNav: AppHomeNav? by remember { mutableStateOf(null) }
    if (selNavIndex != 0 && homeNav != null) {
        BackHandler { selNavIndex = 0; homeNav?.navBack() }
    }
    Scaffold(
        bottomBar = {
            HomeNavigationBar(
                selectedIndex = selNavIndex,
                onSelectItem = { i -> selNavIndex = i; homeNav?.setNavPage(i) },
                windowInsets = WindowInsets(bottom = paddingValues.calculateBottomPadding())
            )
        },
        content = { contentPadding ->
            AndroidFragment<AppHomeNavFragment>(modifier = Modifier.fillMaxSize()) { homeNav = it }
        }
    )
}

@Composable
private fun HomeNavigationBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelectItem: (Int) -> Unit,
    windowInsets: WindowInsets = WindowInsets(0),
) {
    NavigationBar(modifier = modifier, windowInsets = windowInsets) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onSelectItem(0) },
            icon = { Icon(Icons.TwoTone.Home, contentDescription = null) }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onSelectItem(1) },
            icon = { Icon(Icons.AutoMirrored.TwoTone.List, contentDescription = null) }
        )
    }
}

@PreviewCombinedColorLayout
@Composable
private fun AppHomePreview() {
    MaterialTheme {
        Scaffold(
            bottomBar = { HomeNavigationBar(selectedIndex = 0, onSelectItem = {}) },
            content = { _ -> },
        )
    }
}