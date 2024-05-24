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

package com.madness.collision.unit.api_viewing.apps

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Parcel
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppMaintainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Map to maintained apps */
suspend fun List<PackageInfo>.toMtnPkgApps(context: Context, lifecycleOwner: LifecycleOwner) =
    toPkgApps(context, AppMaintainer.get(context, lifecycleOwner))

suspend fun List<PackageInfo>.toPkgApps(context: Context, anApp: ApiViewingApp) = coroutineScope {
    val lastIndex = lastIndex
    mapIndexed { index, packageInfo ->
        async(Dispatchers.Default) {
            val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
            app.init(context, packageInfo, preloadProcess = true, archive = false)
            app
        }
    }.map { it.await() }
}

suspend fun List<*>.toRecApps(context: Context, anApp: ApiViewingApp) = coroutineScope {
    val lastIndex = lastIndex
    mapIndexedNotNull { index, item ->
        if (item !is ApiViewingApp) return@mapIndexedNotNull null
        async(Dispatchers.Default) {
            val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
            item.copyTo(app)
            // get application info one at a time, may be optimized
            app.initIgnored(context)
            app
        }
    }.map { it.await() }
}

fun ApiViewingApp.copyTo(other: ApiViewingApp) {
    val parcel = Parcel.obtain()
    writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    other.readParcel(parcel)
}