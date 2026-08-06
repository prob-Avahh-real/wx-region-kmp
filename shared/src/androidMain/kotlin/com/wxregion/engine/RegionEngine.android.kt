package com.wxregion.engine

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.wxregion.config.RegionConfigStore
import com.wxregion.model.ApplyMethod
import com.wxregion.model.ApplyResult
import com.wxregion.model.CapabilityStatus
import com.wxregion.model.RegionMode
import com.wxregion.platform.AndroidContextHolder
import com.wxregion.platform.ShizukuRegionApplier
import com.wxregion.platform.ShizukuShell
import com.wxregion.platform.WeChatProbe
import com.wxregion.platform.WeChatRegionAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class RegionEngine actual constructor() {
    private val context get() = AndroidContextHolder.appContext
    private val configStore = RegionConfigStore()
    private val shizukuApplier = ShizukuRegionApplier { ShizukuShell.exec(it) }

    actual suspend fun detectCapabilities(): CapabilityStatus = withContext(Dispatchers.Default) {
        val a11y = isAccessibilityServiceEnabled()
        CapabilityStatus(
            wechatInstalled = WeChatProbe.isInstalled(context),
            wechatVersion = WeChatProbe.versionName(context),
            lsposedAvailable = isLsposedModuleActive(),
            accessibilityGranted = a11y,
            shizukuAvailable = ShizukuShell.isAvailable(),
        )
    }

    actual suspend fun apply(mode: RegionMode): ApplyResult = withContext(Dispatchers.Default) {
        if (!WeChatProbe.isInstalled(context)) {
            return@withContext ApplyResult.Failed("未安装微信")
        }

        configStore.syncToLsposedModule(mode)
        val method = configStore.load().method

        when (method) {
            ApplyMethod.LSPOSED -> applyViaLsposed(mode)
            ApplyMethod.ACCESSIBILITY -> applyViaAccessibility(mode)
            ApplyMethod.SHIZUKU -> shizukuApplier.apply(mode)
        }
    }

    private fun applyViaLsposed(mode: RegionMode): ApplyResult {
        configStore.syncToLsposedModule(mode)
        return if (isLsposedModuleActive()) {
            ApplyResult.Success
        } else {
            ApplyResult.NeedsUserAction(
                "配置已写入。请在 LSPosed 中启用「微信地区面具」模块，作用域勾选微信 (${WeChatProbe.WECHAT_PACKAGE})，然后重启微信。",
            )
        }
    }

    private fun applyViaAccessibility(mode: RegionMode): ApplyResult {
        if (!isAccessibilityServiceEnabled()) {
            return ApplyResult.NeedsUserAction("请先在本应用设置中开启无障碍服务。")
        }
        WeChatRegionAccessibilityService.startAutomation(mode)
        WeChatRegionAccessibilityService.openWeChat(context)
        return ApplyResult.NeedsUserAction("已启动无障碍自动化，请保持在微信前台直至完成。")
    }

    actual fun openLsposedScopeHelp() {
        // LSPosed Manager 常见包名
        val candidates = listOf(
            "org.lsposed.manager",
            "io.github.lsposed.manager",
        )
        for (pkg in candidates) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        openUrl("https://modules.lsposed.org/")
    }

    actual fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    actual fun openShizukuInstallHelp() {
        openUrl("https://shizuku.rikka.app/")
    }

    actual fun launchWeChatProfile() {
        WeChatRegionAccessibilityService.openWeChat(context)
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains(context.packageName)
    }

    private fun isLsposedModuleActive(): Boolean {
        return try {
            val clazz = Class.forName("de.robv.android.xposed.XposedBridge")
            clazz.getMethod("getXposedVersion").invoke(null) != null
        } catch (_: Exception) {
            // 主应用无法直接检测模块是否激活，检查是否安装了模块 APK
            isPackageInstalled("com.wxregion.lsposed")
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}
