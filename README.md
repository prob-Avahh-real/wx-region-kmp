# 微信地区面具 (WxRegionMask)

Kotlin Multiplatform 应用：在**任意版本微信 8.x** 上，将个人资料中的**地区**设为：

- **隐藏** — 资料页不显示地区
- **显示中国大陆** — 对外仅展示「中国大陆」

默认交付 **Android APK** + **LSPosed 模块**；iOS 仅提示手动设置（系统限制无法 Hook 微信）。

## 方案

| 方式 | 需要 | 适用 |
|------|------|------|
| **LSPosed 模块** | Root + LSPosed | 任意微信 8.x，Hook 资料页与 region 字段，**推荐** |
| **无障碍自动化** | 开启无障碍 | 无 Root，自动点击「不显示」/「中国大陆」 |
| **Shizuku** | Shizuku 运行中 | 向 `regioncode` 注入自定义条目（部分环境需 Root） |

## 构建

```bash
cd active/wx-region-kmp

# Release APK + Magisk 一键包
./gradlew assembleReleaseAll

# 或分步
./gradlew :composeApp:assembleRelease
./gradlew :lsposed:assembleRelease
./gradlew packageMagiskRelease
```

### 产物

| 文件 | 说明 |
|------|------|
| `composeApp/build/outputs/apk/release/composeApp-release.apk` | 主应用 Release |
| `lsposed/build/outputs/apk/release/lsposed-release.apk` | LSPosed 模块 Release |
| `build/dist/WxRegionMask-magisk-v1.0.0.zip` | **Magisk 刷机包**（含两个 APK） |

### 签名

- 复制 `keystore.properties.example` → `keystore.properties` 并填入正式密钥
- 未配置时 Release 使用 debug 密钥（仅本地测试）

## Magisk 一键安装

1. 将 `WxRegionMask-magisk-v*.zip` 复制到手机
2. Magisk → 模块 → **从本地安装** → 选择 zip
3. **重启设备**（`service.sh` 会自动 `pm install` 两个 APK）
4. LSPosed → 启用「微信地区面具」→ 作用域勾选 **微信**
5. 打开主应用，选择模式并应用

Debug 构建（开发用）：

```bash
./gradlew :composeApp:assembleDebug
./gradlew :lsposed:assembleDebug
```

## 使用（LSPosed，推荐）

1. 安装主应用与 `lsposed-debug.apk`
2. LSPosed → 模块 → 启用「微信地区面具」→ 作用域勾选 **微信** (`com.tencent.mm`)
3. 打开主应用，选择模式，点击「应用设置」
4. 重启微信 → 我 → 头像，查看地区栏

主应用写入的配置通过 `XSharedPreferences` 与模块同步（包名 `com.wxregion.mask`）。

## 使用（无障碍）

1. 主应用 → 无障碍 → 开启「微信地区自动化」
2. 选择模式 → 应用方式选「无障碍自动化」→ 应用设置
3. 保持微信在前台直至自动化完成

## 技术说明

- **UI Hook**：`ContactInfoUI` 等资料页 `onResume` 遍历视图，隐藏或改写「地区」行
- **数据 Hook**：多候选 `Contact` / storage 类的 `getProvince` / `getRegionCode` 等
- **TextView 兜底**：资料页内 `setText` 改写疑似地区字符串
- **Shizuku**：`am force-stop` 后编辑 `/data/data/com.tencent.mm/MicroMsg/regioncode/mmregioncode_zh_CN.txt`

## 限制

- 仅影响**个人资料地区展示**，不修改朋友圈历史位置标签
- iOS / 未越狱设备无法 Hook 微信，请使用微信自带「不显示」或本 Android 方案
- 微信大版本变更后若 Hook 失效，可在 `WxRegionHook.kt` 补充类名

## 包名

- 主应用：`com.wxregion.mask`
- LSPosed 模块：`com.wxregion.lsposed`
