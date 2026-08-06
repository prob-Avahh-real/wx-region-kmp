package com.wxregion.lsposed;

import de.robv.android.xposed.XSharedPreferences;

public final class RegionPrefs {
    private static final String PKG = "com.wxregion.mask";
    private static final String PREFS = "wx_region_config";
    private static final String KEY_MODE = "region_mode";

    private static final XSharedPreferences PREFS_INSTANCE = new XSharedPreferences(PKG, PREFS);

    private RegionPrefs() {}

    public static RegionMode mode() {
        PREFS_INSTANCE.makeWorldReadable();
        PREFS_INSTANCE.reload();
        String value = PREFS_INSTANCE.getString(KEY_MODE, null);
        if ("HIDDEN".equals(value)) {
            return RegionMode.HIDDEN;
        }
        if ("MAINLAND_CHINA".equals(value)) {
            return RegionMode.MAINLAND_CHINA;
        }
        return null;
    }
}
