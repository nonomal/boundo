/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.unit.api_viewing.list.item

import android.content.Context
import android.content.pm.*
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.madness.collision.R
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.Utils
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.X
import java.text.SimpleDateFormat
import java.util.*
import javax.security.cert.X509Certificate

internal class AppItemService {
    private var regexFields: MutableMap<String, String> = HashMap()

    fun getAppDetails(context: Context, appInfo: ApiViewingApp): CharSequence {
        val builder = SpannableStringBuilder()
        val reOne = retrieveOn(context, appInfo, 0, "")
        if (!reOne.first) return ""
        var pi = reOne.second!!
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", SystemUtil.getLocaleApp())
        val spanFlags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        builder.append(context.getString(R.string.apiDetailsPackageName), StyleSpan(Typeface.BOLD), spanFlags)
                .append(pi.packageName ?: "").append('\n')
        builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsVerName), StyleSpan(Typeface.BOLD), spanFlags)
                .append(pi.versionName ?: "")
                .append('\n')
        builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsVerCode), StyleSpan(Typeface.BOLD), spanFlags)
                .append(PackageInfoCompat.getLongVersionCode(pi).toString()).append('\n')
        builder.append(context.getString(R.string.apiSdkTarget), StyleSpan(Typeface.BOLD), spanFlags)
                .append(context.getString(R.string.textColon), StyleSpan(Typeface.BOLD), spanFlags)
                .append(appInfo.targetAPI.toString())
                .append(context.getString(R.string.textParentheses, "${Utils.getAndroidVersionByAPI(appInfo.targetAPI, true)}, ${Utils.getAndroidCodenameByAPI(context, appInfo.targetAPI)}"))
                .append('\n')
        builder.append(context.getString(R.string.apiSdkMin), StyleSpan(Typeface.BOLD), spanFlags)
                .append(context.getString(R.string.textColon), StyleSpan(Typeface.BOLD), spanFlags)
                .append(appInfo.minAPI.toString())
                .append(context.getString(R.string.textParentheses, "${Utils.getAndroidVersionByAPI(appInfo.minAPI, true)}, ${Utils.getAndroidCodenameByAPI(context, appInfo.minAPI)}"))
                .append('\n')
        if (appInfo.isNotArchive) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = pi.firstInstallTime
            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsFirstInstall), StyleSpan(Typeface.BOLD), spanFlags)
                    .append(format.format(cal.time))
                    .append('\n')
            cal.timeInMillis = pi.lastUpdateTime
            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsLastUpdate), StyleSpan(Typeface.BOLD), spanFlags)
                    .append(format.format(cal.time))
                    .append('\n')
            val installer: String? = if (X.aboveOn(X.R)) {
                val si = try {
                    context.packageManager.getInstallSourceInfo(appInfo.packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }
                if (si != null) {
                    // todo initiatingPackageName
//                        builder.append("InitiatingPackageName: ", StyleSpan(Typeface.BOLD), spanFlags)
                    // If the package that requested the install has been uninstalled,
                    // only this app's own install information can be retrieved
//                        builder.append(si.initiatingPackageName ?: "Unknown").append('\n')
//                        builder.append("OriginatingPackageName: ", StyleSpan(Typeface.BOLD), spanFlags)
                    // If not holding the INSTALL_PACKAGES permission then the result will always return null
//                        builder.append(si.originatingPackageName ?: "Unknown").append('\n')
                    si.installingPackageName
                } else {
                    null
                }
            } else {
                getInstallerLegacy(context, appInfo)
            }
            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsInsatllFrom), StyleSpan(Typeface.BOLD), spanFlags)
            if (installer != null) {
                val installerName = MiscApp.getApplicationInfo(context, packageName = installer)
                        ?.loadLabel(context.packageManager)?.toString() ?: ""
                if (installerName.isNotEmpty()) {
                    builder.append(installerName)
                } else {
                    val installerAndroid = ApiViewingApp.packagePackageInstaller
                    val installerGPlay = ApiViewingApp.packagePlayStore
                    when (installer) {
                        installerGPlay ->
                            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsInstallGP))
                        installerAndroid ->
                            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsInstallPI))
                        "null" ->
                            builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsInstallUnknown))
                        else ->
                            builder.append(installer)
                    }
                }
            } else {
                builder.append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsInstallUnknown))
            }
            builder.append('\n')
        }

        if (!appInfo.isNativeLibrariesRetrieved) appInfo.retrieveNativeLibraries()
        val nls = appInfo.nativeLibraries
        builder.append(context.getString(R.string.av_details_native_libs), StyleSpan(Typeface.BOLD), spanFlags)
                .append("armeabi-v7a ").append(if (nls[0]) '✓' else '✗').append("  ")
                .append("arm64-v8a ").append(if (nls[1]) '✓' else '✗').append("  ")
                .append("x86 ").append(if (nls[2]) '✓' else '✗').append("  ")
                .append("x86_64 ").append(if (nls[3]) '✓' else '✗').append("  ")
                .append("Flutter ").append(if (nls[4]) '✓' else '✗').append("  ")
                .append("React Native ").append(if (nls[5]) '✓' else '✗').append("  ")
                .append("Xamarin ").append(if (nls[6]) '✓' else '✗').append("  ")
                .append("Kotlin ").append(if (nls[7]) '✓' else '✗')
                .append('\n')

        var permissions: Array<String> = emptyArray()
        var activities: Array<ActivityInfo> = emptyArray()
        var receivers: Array<ActivityInfo> = emptyArray()
        var services: Array<ServiceInfo> = emptyArray()
        var providers: Array<ProviderInfo> = emptyArray()

        val flagSignature = if (X.aboveOn(X.P)) PackageManager.GET_SIGNING_CERTIFICATES
        else getSigFlagLegacy
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or flagSignature
        val reDetails = retrieveOn(context, appInfo, flags, "details")
        if (reDetails.first) {
            pi = reDetails.second!!
            permissions = pi.requestedPermissions ?: emptyArray()
            activities = pi.activities ?: emptyArray()
            receivers = pi.receivers ?: emptyArray()
            services = pi.services ?: emptyArray()
            providers = pi.providers ?: emptyArray()
        } else {
            retrieveOn(context, appInfo, PackageManager.GET_PERMISSIONS, "permissions").let {
                if (it.first) permissions = it.second!!.requestedPermissions ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_ACTIVITIES, "activities").let {
                if (it.first) activities = it.second!!.activities ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_RECEIVERS, "receivers").let {
                if (it.first) receivers = it.second!!.receivers ?: emptyArray()
            }
            retrieveOn(context, appInfo, PackageManager.GET_SERVICES, "services").let {
                if (it.first) services = it.second!!.services ?: emptyArray()
            }
            retrieveOn(context, appInfo, flagSignature, "signing").let {
                if (it.first) pi = it.second!!
            }
        }

        var signatures: Array<Signature> = emptyArray()
        if (X.aboveOn(X.P)) {
            if (pi.signingInfo != null) {
                signatures = if (pi.signingInfo.hasMultipleSigners()) {
                    pi.signingInfo.apkContentsSigners
                } else {
                    pi.signingInfo.signingCertificateHistory
                }
            }
        } else {
            val piSignature = pi.sigLegacy
            if (piSignature != null) signatures = piSignature
        }
        if (regexFields.isEmpty()) {
            Utils.principalFields(context, regexFields)
        }
        for (s in signatures) {
            val cert: X509Certificate? = X.cert(s)
            if (cert != null) {
                val issuerInfo = Utils.getDesc(regexFields, cert.issuerDN)
                val subjectInfo = Utils.getDesc(regexFields, cert.subjectDN)
                val formerPart = "\n\nX.509 " +
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsCert) +
                        "\nNo." + cert.serialNumber.toString(16).toUpperCase(SystemUtil.getLocaleApp()) +
                        " v" +
                        (cert.version + 1).toString() +
                        '\n' + context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsValiSince)
                builder.append(formerPart, StyleSpan(Typeface.BOLD), spanFlags)
                        .append(format.format(cert.notBefore)).append('\n')
                        .append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsValiUntil), StyleSpan(Typeface.BOLD), spanFlags)
                        .append(format.format(cert.notAfter)).append('\n')
                        .append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsIssuer), StyleSpan(Typeface.BOLD), spanFlags)
                        .append(issuerInfo).append('\n')
                        .append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsSubject), StyleSpan(Typeface.BOLD), spanFlags)
                        .append(subjectInfo).append('\n')
                        .append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsSigAlg), StyleSpan(Typeface.BOLD), spanFlags)
                        .append(cert.sigAlgName).append('\n')
                        .append(context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsSigAlgOID), StyleSpan(Typeface.BOLD), spanFlags)
                        .append(cert.sigAlgOID)
                        .append('\n')
            }
        }

        builder.append("\n\n")
                .append(
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsPermissions),
                        StyleSpan(Typeface.BOLD),
                        spanFlags
                ).append('\n')
        if (permissions.isNotEmpty()) {
            Arrays.sort(permissions)
            for (permission in permissions) {
                builder.append(permission).append('\n')
            }
        } else {
            builder.append(context.getString(R.string.text_no_content)).append('\n')
        }

        builder.append("\n\n")
                .append(
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsActivities),
                        StyleSpan(Typeface.BOLD),
                        spanFlags
                ).append('\n')
        if (activities.isNotEmpty()) {
            Arrays.sort(activities) { o1, o2 -> o1.name.compareTo(o2.name) }
            for (a in activities) {
                builder.append(a.name).append('\n')
            }
        } else {
            builder.append(context.getString(R.string.text_no_content)).append('\n')
        }

        builder.append("\n\n")
                .append(
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsReceivers),
                        StyleSpan(Typeface.BOLD),
                        spanFlags
                ).append('\n')
        if (receivers.isNotEmpty()) {
            Arrays.sort(receivers) { o1, o2 -> o1.name.compareTo(o2.name) }
            for (r in receivers) {
                builder.append(r.name).append('\n')
            }
        } else {
            builder.append(context.getString(R.string.text_no_content)).append('\n')
        }

        builder.append("\n\n")
                .append(
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsServices),
                        StyleSpan(Typeface.BOLD),
                        spanFlags
                ).append('\n')
        if (services.isNotEmpty()) {
            Arrays.sort(services) { o1, o2 -> o1.name.compareTo(o2.name) }
            for (s in services) {
                builder.append(s.name).append('\n')
            }
        } else {
            builder.append(context.getString(R.string.text_no_content)).append('\n')
        }

        builder.append("\n\n")
                .append(
                        context.getString(com.madness.collision.unit.api_viewing.R.string.apiDetailsProviders),
                        StyleSpan(Typeface.BOLD),
                        spanFlags
                ).append('\n')
        if (providers.isNotEmpty()) {
            Arrays.sort(providers) { o1, o2 -> o1.name.compareTo(o2.name) }
            for (p in providers) builder.append(p.name).append('\n')
        } else {
            builder.append(context.getString(R.string.text_no_content)).append('\n')
        }

        return SpannableString.valueOf(builder)
    }

    @Suppress("deprecation")
    private fun getInstallerLegacy(context: Context, app: ApiViewingApp): String? {
        return try {
            context.packageManager.getInstallerPackageName(app.packageName)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("deprecation")
    private val getSigFlagLegacy = PackageManager.GET_SIGNATURES

    @Suppress("deprecation")
    private val PackageInfo.sigLegacy: Array<Signature>?
        get() = signatures

    private fun retrieveOn(context: Context, appInfo: ApiViewingApp, extraFlags: Int, subject: String): Pair<Boolean, PackageInfo?> {
        var pi: PackageInfo? = null
        return try {
            pi = if (appInfo.isArchive) {
                context.packageManager.getPackageArchiveInfo(appInfo.appPackage.basePath, extraFlags)
            } else {
                context.packageManager.getPackageInfo(appInfo.packageName, extraFlags)
            }
            true to pi
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("APIAdapter", String.format("failed to retrieve %s of %s", subject, appInfo.packageName))
            false to pi
        }
    }
}