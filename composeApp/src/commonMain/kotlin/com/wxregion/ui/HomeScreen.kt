package com.wxregion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wxregion.config.RegionConfigStore
import com.wxregion.engine.RegionEngine
import com.wxregion.model.ApplyMethod
import com.wxregion.model.ApplyResult
import com.wxregion.model.RegionMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val configStore = remember { RegionConfigStore() }
    val engine = remember { RegionEngine() }

    var config by remember { mutableStateOf(configStore.load()) }
    var capabilities by remember { mutableStateOf<com.wxregion.model.CapabilityStatus?>(null) }
    var applying by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        capabilities = engine.detectCapabilities()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("微信地区面具") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(capabilities)

            SectionTitle("地区展示")
            RegionMode.entries.forEach { mode ->
                SelectRow(
                    selected = config.mode == mode,
                    title = mode.label,
                    subtitle = when (mode) {
                        RegionMode.HIDDEN -> "资料页不显示地区字段"
                        RegionMode.MAINLAND_CHINA -> "对外仅显示「中国大陆」"
                    },
                    onClick = {
                        config = config.copy(mode = mode)
                        configStore.save(config)
                    },
                )
            }

            SectionTitle("应用方式")
            ApplyMethod.entries.forEach { method ->
                SelectRow(
                    selected = config.method == method,
                    title = method.label,
                    subtitle = method.description,
                    onClick = {
                        config = config.copy(method = method)
                        configStore.save(config)
                    },
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        applying = true
                        statusMessage = null
                        val result = engine.apply(config.mode)
                        applying = false
                        statusMessage = when (result) {
                            ApplyResult.Success -> "已生效，重启微信后查看资料页。"
                            is ApplyResult.NeedsUserAction -> result.message
                            is ApplyResult.Failed -> result.message
                        }
                        snackbar.showSnackbar(statusMessage ?: "")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !applying && capabilities?.wechatInstalled != false,
            ) {
                if (applying) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("应用设置")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { engine.openLsposedScopeHelp() }) {
                    Text("LSPosed")
                }
                OutlinedButton(onClick = { engine.openAccessibilitySettings() }) {
                    Text("无障碍")
                }
                OutlinedButton(onClick = { engine.openShizukuInstallHelp() }) {
                    Text("Shizuku")
                }
            }

            OutlinedButton(
                onClick = { engine.launchWeChatProfile() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开微信")
            }

            statusMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("说明", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• LSPosed：适配任意微信 8.x，Hook 资料页展示，推荐。\n" +
                            "• 无障碍：无需 Root，自动点击「不显示」或「中国大陆」。\n" +
                            "• Shizuku：向微信注入自定义地区条目（需可写 /data/data）。\n" +
                            "• iOS 请在微信内手动设置地区。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SelectRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusCard(capabilities: com.wxregion.model.CapabilityStatus?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("环境检测", style = MaterialTheme.typography.titleSmall)
            if (capabilities == null) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text("微信：${if (capabilities.wechatInstalled) "已安装 ${capabilities.wechatVersion ?: ""}" else "未安装"}")
                Text("LSPosed 模块：${if (capabilities.lsposedAvailable) "已就绪" else "未检测到"}")
                Text("无障碍：${if (capabilities.accessibilityGranted) "已开启" else "未开启"}")
                Text("Shizuku：${if (capabilities.shizukuAvailable) "可用" else "不可用"}")
            }
        }
    }
}
