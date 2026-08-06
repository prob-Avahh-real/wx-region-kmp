package com.wxregion.platform

import com.wxregion.model.ApplyResult
import com.wxregion.model.RegionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 通过 Shizuku shell 注入微信地区码文件，配合后续选择 */
class ShizukuRegionApplier(
    private val shell: suspend (String) -> ShellResult,
) {
    suspend fun apply(mode: RegionMode): ApplyResult = withContext(Dispatchers.IO) {
        val stop = shell("am force-stop ${WeChatProbe.WECHAT_PACKAGE}")
        if (!stop.ok) {
            return@withContext ApplyResult.Failed("无法停止微信：${stop.stderr}")
        }

        when (mode) {
            RegionMode.HIDDEN -> {
                // 旧版微信无「不显示」时，依赖 LSPosed；Shizuku 路径尝试清空 regioncode 自定义行
                val clear = shell(
                    """
                    REGION="/data/data/${WeChatProbe.WECHAT_PACKAGE}/MicroMsg/regioncode/mmregioncode_zh_CN.txt"
                    if [ -f "${'$'}REGION" ]; then
                      grep -v '^WRM|' "${'$'}REGION" > "${'$'}REGION.bak" 2>/dev/null || cp "${'$'}REGION" "${'$'}REGION.bak"
                      mv "${'$'}REGION.bak" "${'$'}REGION"
                      chmod 660 "${'$'}REGION"
                      chown u0_a$(cmd package list packages -U ${WeChatProbe.WECHAT_PACKAGE} | grep -oE '[0-9]+$') "${'$'}REGION" 2>/dev/null || true
                    fi
                    """.trimIndent(),
                )
                if (!clear.ok) {
                    return@withContext ApplyResult.NeedsUserAction(
                        "Shizuku 无法写入微信数据目录。请改用 LSPosed 模块，或在微信内手动选择「不显示」。",
                    )
                }
                ApplyResult.NeedsUserAction("已清理自定义地区条目。请打开微信 → 我 → 头像 → 地区 → 选择「不显示」。")
            }

            RegionMode.MAINLAND_CHINA -> {
                val inject = shell(
                    """
                    REGION="/data/data/${WeChatProbe.WECHAT_PACKAGE}/MicroMsg/regioncode/mmregioncode_zh_CN.txt"
                    if [ ! -f "${'$'}REGION" ]; then
                      mkdir -p "/data/data/${WeChatProbe.WECHAT_PACKAGE}/MicroMsg/regioncode" 2>/dev/null
                      touch "${'$'}REGION"
                    fi
                    grep -q '^WRM|中国大陆' "${'$'}REGION" 2>/dev/null || sed -i '1iWRM|中国大陆' "${'$'}REGION" 2>/dev/null || echo 'WRM|中国大陆' | cat - "${'$'}REGION" > "${'$'}REGION.new" && mv "${'$'}REGION.new" "${'$'}REGION"
                    chmod 660 "${'$'}REGION"
                    """.trimIndent(),
                )
                if (!inject.ok) {
                    return@withContext ApplyResult.NeedsUserAction(
                        "无法注入地区文件（需 Shizuku + 可访问 /data/data）。请使用 LSPosed 或无障碍方案。",
                    )
                }
                ApplyResult.NeedsUserAction(
                    "已在地区列表注入「中国大陆」。请打开微信 → 我 → 头像 → 地区 → 选择「中国大陆」并保存。",
                )
            }
        }
    }
}

data class ShellResult(val ok: Boolean, val stdout: String, val stderr: String)
