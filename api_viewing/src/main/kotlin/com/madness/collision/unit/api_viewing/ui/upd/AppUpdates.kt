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

package com.madness.collision.unit.api_viewing.ui.upd

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.chief.app.BoundoTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackageInfo
import com.madness.collision.unit.api_viewing.data.VerInfo
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import com.madness.collision.util.dev.PreviewCombinedColorLayout

@Composable
fun AppUpdatesPage(paddingValues: PaddingValues) {
    val viewModel: AppUpdatesViewModel = viewModel()
    val sections by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            UpdatesAppBar(windowInsets = WindowInsets(top = paddingValues.calculateTopPadding()))
        },
        content = { contentPadding ->
            if (sections.isNotEmpty()) {
                UpdatesList(
                    sections = sections,
                    paddingValues = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                )
            } else {
                Text("Nada")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatesAppBar(windowInsets: WindowInsets = WindowInsets(0)) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
        },
        windowInsets = windowInsets,
    )
}

@Composable
private fun UpdatesList(sections: AppUpdatesUiState, paddingValues: PaddingValues) {
    LazyColumn(contentPadding = paddingValues) {
        for ((secIndex, secList) in sections) {
            if (secList.isNotEmpty()) {
                item(key = secIndex) {
                    UpdateSectionTitle(text = updateIndexLabel(secIndex))
                }
                sectionItems(secIndex, secList)
            }
        }
    }
}

private fun LazyListScope.sectionItems(
    secIndex: AppUpdatesIndex,
    secList: List<*>,
) {
    if (secIndex == AppUpdatesIndex.UPG) {
        items(secList) { upd ->
            if (upd is Upgrade) {
                val context = LocalContext.current
                AppUpdateItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    name = upd.new.name,
                    apiInfo = remember(upd) { VerInfo.targetDisplay(upd.new) },
                    iconInfo = remember(upd) { AppPackageInfo(context, upd.new) },
                    newApi = remember(upd) { VerInfo(upd.targetApi.second) },
                    oldApi = remember(upd) { VerInfo(upd.targetApi.first) },
                    newVer = remember(upd) { AppInstallVersion(upd.versionCode.second, upd.versionName.second, "") },
                    oldVer = remember(upd) { AppInstallVersion(upd.versionCode.first, upd.versionName.first, "") },
                    newTimestamp = upd.updateTime.second,
                    oldTimestamp = upd.updateTime.first,
                )
            }
        }
    } else {
        items(secList) { app ->
            if (app is ApiViewingApp) {
                val context = LocalContext.current
                AppItem(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    name = app.name,
                    timestamp = app.updateTime,
                    apiInfo = remember(app) { VerInfo.targetDisplay(app) },
                    iconInfo = remember(app) { AppPackageInfo(context, app) }
                )
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun updateIndexLabel(index: AppUpdatesIndex): String {
    return stringResource(when (index) {
        AppUpdatesIndex.NEW -> R.string.av_upd_new
        AppUpdatesIndex.UPG -> R.string.av_upd_upg
        AppUpdatesIndex.VER -> R.string.av_upd_ver_upd
        AppUpdatesIndex.PCK -> R.string.av_upd_pck_upd
        AppUpdatesIndex.REC -> R.string.av_updates_recents
        AppUpdatesIndex.USE -> R.string.av_upd_used
    })
}

@Composable
private fun UpdateSectionTitle(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        text = text,
        fontSize = 13.sp,
        lineHeight = 13.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
@PreviewCombinedColorLayout
private fun AppUpdatesPreview() {
    BoundoTheme {
        Scaffold(
            topBar = { UpdatesAppBar() },
            content = { contentPadding ->
                UpdatesList(
                    sections = emptyMap(),
                    paddingValues = PaddingValues(top = contentPadding.calculateTopPadding()),
                )
            }
        )
    }
}
