# wx-region-kmp

微信地区面具 (WxRegionMask) Kotlin Multiplatform 应用：在**任意版本微信 8.x** 上，将个人资料中的**地区**设为： - **隐藏** — 资料页不显示地区 - **显示中国大陆** — 对外仅展示「中国大陆」 默认交付 **Android APK** + **LSPosed 模块**；iOS 仅提示手动设置（系统限制无法 Hook 微信）。 方案 构建 cd active/wx-region-kmp

## 用途与 Target 判定

微信地区面具 + LSPosed，系统模组

- 移动端：需要
- 智能硬件：不适用

## 目标平台

| 平台 | 方式 | 产出 |
|------|------|------|
| 移动端 | KMP / Compose | APK（及既有 iOS 目标） |

## 技术栈信号

- 检测到：kmp

## 命令

```bash
./gradlew :composeApp:assembleDebug
# iOS: open iosApp in Xcode
```

## 助手约束

- Loop：先定可验证 Done 与停止条件；每切片过验证环；失败回灌；详见 docs/LOOP_ENGINEERING.md
- Harness：遵循根 AGENTS「Harness Engineering」；Guide=本文件，Sensor=测试/CI；同类失误二次须沉淀规则或测试（棘轮）。详见 docs/HARNESS_ENGINEERING.md
- 遵循仓库根 `AGENTS.md`：通用性/扩展性、分阶段里程碑、默认双 Target（**仅当用途适合**）
- 可选 Target 未做时可搁置；不要为不适用形态硬加 APK/穿戴
- 改动保持聚焦；公共逻辑抽到 shared/`tools/`，禁止静默失败
