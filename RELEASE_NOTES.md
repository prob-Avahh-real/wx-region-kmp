# WxRegionMask v1.0.1 发布说明

## 更新内容
- 完成 Android Release 签名配置
- 支持生成主应用 APK、LSPosed 模块 APK 和 Magisk 一键安装包
- 更新版本号为 1.0.1

## 安装方式

### 方式 1：直接安装 APK
1. 安装主应用 APK：composeApp/build/outputs/apk/release/composeApp-release.apk
2. 安装 LSPosed 模块 APK：lsposed/build/outputs/apk/release/lsposed-release.apk
3. 打开主应用并应用设置

### 方式 2：Magisk 一键安装
1. 将 build/dist/WxRegionMask-magisk-v1.0.1.zip 复制到手机
2. 在 Magisk 中选择“从本地安装”
3. 重启设备

## 构建命令
```bash
cd active/wx-region-kmp
./gradlew assembleReleaseAll
```
