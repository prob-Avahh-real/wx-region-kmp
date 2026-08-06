package com.wxregion.config

import android.content.Context
import android.content.SharedPreferences
import com.wxregion.model.ApplyMethod
import com.wxregion.model.RegionConfig
import com.wxregion.model.RegionMode
import com.wxregion.platform.AndroidContextHolder

actual class RegionConfigStore actual constructor() {
    private val prefs: SharedPreferences by lazy {
        AndroidContextHolder.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): RegionConfig = RegionConfig(
        mode = RegionMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) }
            ?: RegionMode.HIDDEN,
        method = ApplyMethod.entries.firstOrNull { it.name == prefs.getString(KEY_METHOD, null) }
            ?: defaultApplyMethod(),
        lsposedModuleEnabled = prefs.getBoolean(KEY_LSPOSED, false),
        accessibilityGranted = prefs.getBoolean(KEY_A11Y, false),
        shizukuAvailable = prefs.getBoolean(KEY_SHIZUKU, false),
    )

    actual fun save(config: RegionConfig) {
        prefs.edit()
            .putString(KEY_MODE, config.mode.name)
            .putString(KEY_METHOD, config.method.name)
            .putBoolean(KEY_LSPOSED, config.lsposedModuleEnabled)
            .putBoolean(KEY_A11Y, config.accessibilityGranted)
            .putBoolean(KEY_SHIZUKU, config.shizukuAvailable)
            .apply()
        syncToLsposedModule(config.mode)
    }

    actual fun syncToLsposedModule(mode: RegionMode) {
        // LSPosed 模块通过 XSharedPreferences 读取同名 prefs
        prefs.edit()
            .putString(KEY_MODE, mode.name)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        try {
            @Suppress("DEPRECATION")
            prefs.javaClass.getMethod("commit").invoke(prefs)
        } catch (_: Exception) {
            // ignore
        }
    }

    companion object {
        const val PREFS_NAME = "wx_region_config"
        const val KEY_MODE = "region_mode"
        const val KEY_METHOD = "apply_method"
        const val KEY_LSPOSED = "lsposed_enabled"
        const val KEY_A11Y = "accessibility_granted"
        const val KEY_SHIZUKU = "shizuku_available"
        const val KEY_UPDATED_AT = "updated_at"
    }
}

actual fun defaultApplyMethod(): ApplyMethod = ApplyMethod.LSPOSED
