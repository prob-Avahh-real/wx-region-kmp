package com.wxregion.platform

import android.content.Context
import android.content.pm.PackageManager

object WeChatProbe {
    const val WECHAT_PACKAGE = "com.tencent.mm"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(WECHAT_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun versionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(WECHAT_PACKAGE, 0).versionName
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun versionCode(context: Context): Long = try {
        val info = context.packageManager.getPackageInfo(WECHAT_PACKAGE, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        0L
    }
}
