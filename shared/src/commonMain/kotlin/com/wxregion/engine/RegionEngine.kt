package com.wxregion.engine

import com.wxregion.model.ApplyResult
import com.wxregion.model.CapabilityStatus
import com.wxregion.model.RegionMode

/** 检测环境与执行地区策略 */
expect class RegionEngine() {
    suspend fun detectCapabilities(): CapabilityStatus
    suspend fun apply(mode: RegionMode): ApplyResult
    fun openLsposedScopeHelp()
    fun openAccessibilitySettings()
    fun openShizukuInstallHelp()
    fun launchWeChatProfile()
}
