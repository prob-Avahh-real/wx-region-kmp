#!/system/bin/sh
# Magisk 模块安装脚本 — 由 module_installer.sh 调用

ui_print ""
ui_print "*******************************"
ui_print "  微信地区面具 · Magisk 模块"
ui_print "*******************************"
ui_print ""
ui_print "- 主应用: com.wxregion.mask"
ui_print "- LSPosed: com.wxregion.lsposed"
ui_print ""
ui_print "重启后将自动安装 APK。"
ui_print "请在 LSPosed 中启用模块并勾选微信。"
ui_print ""

# 确保 APK 可读
set_perm_recursive "$MODPATH/common" 0 0 0755 0644

# 清除旧安装标记，升级时可重新安装
rm -f "$MODPATH/common/.installed"

ui_print "安装完成，请重启设备。"
