package com.wxregion.platform

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.wxregion.config.RegionConfigStore
import com.wxregion.model.RegionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 无障碍自动化：在微信「个人信息 → 地区」流程中点击「不显示」或「中国大陆」。
 * 兼容多版本：通过文本/内容描述模糊匹配节点。
 */
class WeChatRegionAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _running.value = true
    }

    override fun onDestroy() {
        _running.value = false
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != WeChatProbe.WECHAT_PACKAGE) return
        if (_pendingMode.value == null) return

        val root = rootInActiveWindow ?: return
        try {
            runAutomation(root, _pendingMode.value!!)
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    private fun runAutomation(root: AccessibilityNodeInfo, mode: RegionMode) {
        when (_step.value) {
            Step.IDLE -> {
                if (clickByTexts(root, listOf("我", "Me"))) {
                    _step.value = Step.PROFILE
                }
            }
            Step.PROFILE -> {
                // 头像区域或「个人信息」
                if (clickByTexts(root, listOf("个人信息", "更多", "地区"))) {
                    _step.value = Step.REGION_ENTRY
                }
            }
            Step.REGION_ENTRY -> {
                if (clickByTexts(root, listOf("地区", "Region"))) {
                    _step.value = Step.REGION_PICKER
                }
            }
            Step.REGION_PICKER -> when (mode) {
                RegionMode.HIDDEN -> {
                    if (clickByTexts(root, listOf("不显示", "隐藏地区", "Do not show", "Don't show"))) {
                        finishSuccess()
                    } else if (clickByTexts(root, listOf("完成", "保存", "Done", "Save"))) {
                        finishSuccess()
                    }
                }
                RegionMode.MAINLAND_CHINA -> {
                    if (clickByTexts(root, listOf("中国大陆", "中国", "China", "WRM|中国大陆"))) {
                        if (clickByTexts(root, listOf("完成", "保存", "Done", "Save"))) {
                            finishSuccess()
                        }
                    } else {
                        // 仅显示国家 trick：选中国 → 选省 → 返回 → 完成
                        if (clickByTexts(root, listOf("中国", "China"))) {
                            _step.value = Step.CHINA_PROVINCE
                        }
                    }
                }
            }
            Step.CHINA_PROVINCE -> {
                // 任意省份
                val province = findClickableWithText(root) { text ->
                    text.length in 2..8 && !text.contains("中国") && !text.contains("完成")
                }
                if (province != null) {
                    province.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    province.recycle()
                    _step.value = Step.CHINA_BACK
                }
            }
            Step.CHINA_BACK -> {
                if (performGlobalAction(GLOBAL_ACTION_BACK)) {
                    _step.value = Step.CHINA_DONE
                }
            }
            Step.CHINA_DONE -> {
                if (clickByTexts(root, listOf("完成", "保存", "Done", "Save"))) {
                    finishSuccess()
                }
            }
        }
    }

    private fun finishSuccess() {
        _lastMessage.value = "已在微信内完成地区设置"
        _pendingMode.value = null
        _step.value = Step.IDLE
    }

    private fun clickByTexts(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                val clickable = findClickableParent(node) ?: node
                if (clickable.isClickable && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    nodes.forEach { it.recycle() }
                    node.recycle()
                    return true
                }
                node.recycle()
            }
            nodes.forEach { it.recycle() }
        }
        return false
    }

    private fun findClickableWithText(
        root: AccessibilityNodeInfo,
        predicate: (String) -> Boolean,
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString().orEmpty()
            if (text.isNotEmpty() && predicate(text) && node.isClickable) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            if (node !== root) node.recycle()
        }
        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(6) {
            current = current?.parent
            if (current?.isClickable == true) return current
        }
        return null
    }

    enum class Step {
        IDLE, PROFILE, REGION_ENTRY, REGION_PICKER, CHINA_PROVINCE, CHINA_BACK, CHINA_DONE,
    }

    companion object {
        private val _running = MutableStateFlow(false)
        private val _pendingMode = MutableStateFlow<RegionMode?>(null)
        private val _step = MutableStateFlow(Step.IDLE)
        private val _lastMessage = MutableStateFlow<String?>(null)

        val running: StateFlow<Boolean> = _running
        val lastMessage: StateFlow<String?> = _lastMessage

        fun startAutomation(mode: RegionMode) {
            _pendingMode.value = mode
            _step.value = Step.IDLE
            _lastMessage.value = "正在打开微信并自动化设置地区…"
        }

        fun openWeChat(context: android.content.Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(WeChatProbe.WECHAT_PACKAGE)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
        }
    }
}
