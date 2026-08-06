package com.wxregion.engine

import com.wxregion.model.ApplyResult
import com.wxregion.model.CapabilityStatus
import com.wxregion.model.RegionMode

actual class RegionEngine actual constructor() {
    actual suspend fun detectCapabilities(): CapabilityStatus = CapabilityStatus(
        wechatInstalled = true,
        wechatVersion = null,
        lsposedAvailable = false,
        accessibilityGranted = false,
        shizukuAvailable = false,
    )

    actual suspend fun apply(mode: RegionMode): ApplyResult =
        ApplyResult.Failed("iOS 无法修改微信客户端资料，请在微信内手动设置：我 → 头像 → 地区")

    actual fun openLsposedScopeHelp() = Unit
    actual fun openAccessibilitySettings() = Unit
    actual fun openShizukuInstallHelp() = Unit
    actual fun launchWeChatProfile() = Unit
}
