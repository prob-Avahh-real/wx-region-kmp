package com.wxregion.model

import kotlinx.serialization.Serializable

/** 微信个人资料地区展示模式 */
@Serializable
enum class RegionMode(val label: String, val displayText: String?) {
    /** 资料页不显示地区 */
    HIDDEN("隐藏地区", null),

    /** 仅显示「中国大陆」 */
    MAINLAND_CHINA("显示中国大陆", "中国大陆"),
}

/** 应用所选策略 */
@Serializable
enum class ApplyMethod(val label: String, val description: String) {
    LSPOSED(
        label = "LSPosed 模块",
        description = "跨版本 Hook 微信资料页，推荐。需安装 LSPosed 并在作用域勾选微信。",
    ),
    ACCESSIBILITY(
        label = "无障碍自动化",
        description = "通过无障碍服务在微信内自动完成地区设置，无需 Root。",
    ),
    SHIZUKU(
        label = "Shizuku + 地区文件",
        description = "注入自定义地区条目后配合自动化选择，需 Shizuku（部分机型需 Root）。",
    ),
}

@Serializable
data class RegionConfig(
    val mode: RegionMode = RegionMode.HIDDEN,
    val method: ApplyMethod = ApplyMethod.LSPOSED,
    val lsposedModuleEnabled: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val shizukuAvailable: Boolean = false,
)

sealed class ApplyResult {
    data object Success : ApplyResult()
    data class NeedsUserAction(val message: String) : ApplyResult()
    data class Failed(val message: String) : ApplyResult()
}

data class CapabilityStatus(
    val wechatInstalled: Boolean,
    val wechatVersion: String?,
    val lsposedAvailable: Boolean,
    val accessibilityGranted: Boolean,
    val shizukuAvailable: Boolean,
)
