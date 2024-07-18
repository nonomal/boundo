/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.*
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.info.AppType
import com.madness.collision.unit.api_viewing.info.PkgInfo
import com.madness.collision.unit.api_viewing.info.getAppType
import com.madness.collision.unit.api_viewing.info.isOnBackInvokedCallbackEnabled
import com.madness.collision.unit.api_viewing.util.ApkUtil
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.os.OsUtils

data class ApiViewingIconInfo(
    @Embedded(prefix = "icS_")  // SystemICon from API (modified by system, may be from an icon pack)
    val system: ApiViewingIconDetails,
    @Embedded(prefix = "icN_")  // NormalICon from APK (unmodified original app icon)
    val normal: ApiViewingIconDetails,
    @Embedded(prefix = "icR_")  // RoundICon from APK (unmodified original app icon)
    val round: ApiViewingIconDetails,
)

class ApiViewingIconDetails(val isDefined: Boolean, val isAdaptive: Boolean)

@Entity(tableName = "app")
open class ApiViewingApp(@PrimaryKey @ColumnInfo var packageName: String) : Cloneable {

    companion object {
        const val packagePlayStore = "com.android.vending"
        const val packagePackageInstaller = "com.google.android.packageinstaller"
    }

    fun assignPersistentFieldsFrom(other: ApiViewingApp, deepCopy: Boolean) {
        packageName = other.packageName
        verName = other.verName
        verCode = other.verCode
        compileAPI = other.compileAPI
        compileApiCodeName = other.compileApiCodeName
        targetAPI = other.targetAPI
        minAPI = other.minAPI
        apiUnit = other.apiUnit
        updateTime = other.updateTime
        isNativeLibrariesRetrieved = other.isNativeLibrariesRetrieved
        nativeLibraries = if (!deepCopy) other.nativeLibraries else other.nativeLibraries.copyOf()
        isLaunchable = other.isLaunchable
        appPackage = if (!deepCopy) other.appPackage else AppPackage(other.appPackage.apkPaths)
        jetpackComposed = other.jetpackComposed
        iconInfo = if (!deepCopy) other.iconInfo else other.iconInfo?.copy()
    }

    var verName: String = ""
    var verCode: Long = 0L
    @Ignore var compileAPI: Int = -1
    @Ignore var compileApiCodeName: String? = null
    var targetAPI: Int = -1
    var minAPI: Int = -1
    var apiUnit: Int = ApiUnit.NON
    var updateTime: Long = 0L
    var isNativeLibrariesRetrieved: Boolean = false
    var nativeLibraries: BooleanArray = BooleanArray(ApkUtil.NATIVE_LIB_SUPPORT_SIZE) { false }
    var isLaunchable: Boolean = false
    var appPackage: AppPackage = AppPackage("")
    @ColumnInfo(defaultValue = "-1")
    var jetpackComposed: Int = -1
    @Embedded
    var iconInfo: ApiViewingIconInfo? = null

    @Ignore var uid: Int = -1
    @Ignore var name: String = ""
    @Ignore var targetSDK: String = ""
    @Ignore var minSDK: String = ""
    @Ignore var targetSDKDisplay: String = ""
    @Ignore var minSDKDisplay: String = ""
    @Ignore var targetSDKDouble: Double = -1.0
    @Ignore var minSDKDouble: Double = -1.0
    @Ignore var targetSDKLetter: Char = '?'
    @Ignore var minSDKLetter: Char = '?'

    val adaptiveIcon: Boolean
        get() = iconInfo?.run { listOf(system, normal, round).any { it.isDefined && it.isAdaptive } } == true
    @Ignore var appType: AppType = AppType.Common
    @Ignore var moduleInfo: ModuleInfo? = null
    @Ignore var isCoreApp: Boolean? = null
    @Ignore var isBackCallbackEnabled: Boolean? = null
    @Ignore var category: Int? = null

    val isJetpackComposed: Boolean
        get() = jetpackComposed == 1
    val isThirdPartyPackagesRetrieved: Boolean
        get() = jetpackComposed == 1 || jetpackComposed == 0
    val isArchive: Boolean
        get() = apiUnit == ApiUnit.APK
    val isNotArchive: Boolean
        get() = !isArchive
    val hasIcon: Boolean get() = iconInfo != null

    constructor(): this("")

    constructor(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean)
            : this(info.packageName ?: "") {
        init(context, info, preloadProcess, archive)
    }

    public override fun clone(): Any {
        return (super.clone() as ApiViewingApp).apply {
            nativeLibraries = nativeLibraries.copyOf()
        }
    }

    fun initIgnored(context: Context, pkgInfo: PackageInfo? = getPackageInfo(context)) {
        uid = -1
        compileAPI = -1
        compileApiCodeName = null
        name = ""
        initApiVer()
        appType = AppType.Common
        moduleInfo = null
        isCoreApp = null
        isBackCallbackEnabled = null
        category = null

        pkgInfo?.let { loadFreshProperties(it, context) }
    }

    fun init(context: Context, info: PackageInfo, preloadProcess: Boolean, archive: Boolean) {
        // preloadProcess is always true
        if (!preloadProcess) return
        packageName = info.packageName
        verName = info.versionName ?: ""
        verCode = PackageInfoCompat.getLongVersionCode(info)
        updateTime = info.lastUpdateTime
        apiUnit = when {
            archive -> ApiUnit.APK
            (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 -> ApiUnit.SYS
            else -> ApiUnit.USER
        }
        appPackage = AppPackage(info.applicationInfo)
        targetAPI = info.applicationInfo.targetSdkVersion
        minAPI = when {
            OsUtils.satisfy(OsUtils.N) -> info.applicationInfo.minSdkVersion
            else -> ManifestUtil.getMinSdk(appPackage.basePath).toIntOrNull() ?: -1 // fix cloneable
        }
        isLaunchable = context.packageManager.getLaunchIntentForPackage(packageName) != null

        loadFreshProperties(info, context)
        initApiVer()
    }

    private fun initApiVer() {
        // target API ver
        targetSDK = Utils.getAndroidVersionByAPI(targetAPI, false)
        targetSDKDouble = if (targetSDK.isNotEmpty()) targetSDK.toDouble() else -1.0  // fix cloneable
        targetSDKDisplay = targetSDK
        targetSDKLetter = Utils.getAndroidLetterByAPI(targetAPI)

        // min API ver
        minSDK = Utils.getAndroidVersionByAPI(minAPI, false)
        minSDKDouble = if (minSDK.isNotEmpty()) minSDK.toDouble() else -1.0  // fix cloneable
        minSDKDisplay = minSDK
        minSDKLetter = Utils.getAndroidLetterByAPI(minAPI)
    }

    private fun loadFreshProperties(pkgInfo: PackageInfo, context: Context) {
        val applicationInfo = pkgInfo.applicationInfo
        uid = applicationInfo.uid
        name = AppInfoProcessor.loadName(context, applicationInfo, false)
        if (OsUtils.satisfy(OsUtils.S)) {
            compileAPI = applicationInfo.compileSdkVersion
            compileApiCodeName = applicationInfo.compileSdkVersionCodename
        } else {
            compileAPI = ManifestUtil.getCompileSdk(appPackage.basePath).toIntOrNull() ?: -1 // fix cloneable
        }
        appType = getAppType(pkgInfo)
        // todo enhance module and isOnBackInvokedCallbackEnabled checking performance
        moduleInfo = PkgInfo.getModuleInfo(packageName, context)
        isCoreApp = PkgInfo.getIsCoreApp(pkgInfo)
        isBackCallbackEnabled = isOnBackInvokedCallbackEnabled(pkgInfo.applicationInfo, context)
        category = if (OsUtils.satisfy(OsUtils.O)) pkgInfo.applicationInfo.category else null
    }

    fun getApplicationInfo(context: Context): ApplicationInfo? {
        return if (this.isArchive) MiscApp.getApplicationInfo(context, apkPath = appPackage.basePath)
        else MiscApp.getApplicationInfo(context, packageName = packageName)
    }

    fun getPackageInfo(context: Context): PackageInfo? {
        return if (this.isArchive) MiscApp.getPackageArchiveInfo(context, path = appPackage.basePath)
        else MiscApp.getPackageInfo(context, packageName = packageName)
    }

    fun retrieveAppIconInfo(iconSet: List<Drawable?>) {
        retrieveConsuming(2, iconSet)
    }

    fun retrieveThirdPartyPackages() {
        if (isThirdPartyPackagesRetrieved) return
        retrieveConsuming(1)
    }

    fun retrieveNativeLibraries() {
        if (isNativeLibrariesRetrieved) return
        retrieveConsuming(0)
    }

    open fun retrieveConsuming(target: Int, arg: Any? = null) {
        when (target) {
            0 -> loadNativeLibraries()
            1 -> loadThirdPartyPackages()
            2 -> loadAppIconInfo((arg as List<*>).filterIsInstance<Drawable?>())
        }
    }

    private fun checkKotlin() {  // todo improve Kotlin detection
        appPackage.apkPaths.any { ApkUtil.checkPkg(it, "kotlin") }
    }
}


private fun ApiViewingApp.loadNativeLibraries() {
    fun AppPackage.getNativeLibSupport(): BooleanArray {
        if (!hasSplits) return ApkUtil.getNativeLibSupport(basePath)
        var arr = BooleanArray(0)
        for (path in apkPaths) {
            val lib = ApkUtil.getNativeLibSupport(path)
            if (arr.size != lib.size) { arr = lib; continue }
            for (i in arr.indices) if (!arr[i]) arr[i] = lib[i]
        }
        return arr
    }
    synchronized(nativeLibraries) {
        if (isNativeLibrariesRetrieved) return
        appPackage.getNativeLibSupport().copyInto(nativeLibraries)
        isNativeLibrariesRetrieved = true
    }
}

private fun ApiViewingApp.loadThirdPartyPackages() {
    // ensure thread safety, otherwise encounter exceptions during app list loading
    synchronized(jetpackComposed) {
        if (isThirdPartyPackagesRetrieved) return
        val checkResult = appPackage.apkPaths.any {
            ApkUtil.checkPkg(it, "androidx.compose")
        }
        jetpackComposed = if (checkResult) 1 else 0
    }
}

private fun ApiViewingApp.loadAppIconInfo(iconSet: List<Drawable?>) {
    synchronized(this) {
        if (iconInfo != null) return
        val list = (0..2).map { iconSet.getOrNull(it) }.map { ic ->
            val isDefined = ic != null
            val isAdaptive = isDefined && OsUtils.satisfy(OsUtils.O) && ic is AdaptiveIconDrawable
            ApiViewingIconDetails(isDefined, isAdaptive)
        }
        iconInfo = ApiViewingIconInfo(system = list[0], normal = list[1], round = list[2])
    }
}
