#!/system/bin/sh
# 首次启动后静默安装 APK（Magisk service.d）

MODDIR=${0%/*}
FLAG="$MODDIR/common/.installed"
MASK_APK="$MODDIR/common/wx-region-mask.apk"
LSPOSED_APK="$MODDIR/common/wx-region-lsposed.apk"

[ -f "$FLAG" ] && exit 0

# 等待系统就绪
i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$i" -lt 120 ]; do
  sleep 2
  i=$((i + 1))
done

sleep 5

install_one() {
  apk="$1"
  [ -f "$apk" ] || return 1
  pm install -r -d -g "$apk" >/dev/null 2>&1 \
    || pm install -r -g "$apk" >/dev/null 2>&1 \
    || pm install -r "$apk" >/dev/null 2>&1
}

install_one "$MASK_APK"
install_one "$LSPOSED_APK"

touch "$FLAG"
