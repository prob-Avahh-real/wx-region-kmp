package com.wxregion.lsposed;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook WeChat profile region display across 8.x builds.
 */
public class WxRegionHook implements IXposedHookLoadPackage {

    private static final String WECHAT_PKG = "com.tencent.mm";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!WECHAT_PKG.equals(lpparam.packageName)) {
            return;
        }
        hookProfileUi(lpparam);
        hookRegionGetters(lpparam);
    }

    private void hookProfileUi(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] candidates = {
            "com.tencent.mm.plugin.profile.ui.ContactInfoUI",
            "com.tencent.mm.plugin.profile.ui.ModContactInfoUI",
            "com.tencent.mm.plugin.profile.ui.ContactMoreInfoUI",
        };
        for (String name : candidates) {
            try {
                XposedHelpers.findAndHookMethod(
                    name,
                    lpparam.classLoader,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof Activity activity)) {
                                return;
                            }
                            View decor = activity.getWindow() != null
                                ? activity.getWindow().getDecorView()
                                : null;
                            if (decor == null) {
                                return;
                            }
                            decor.post(() -> applyUiMask(activity));
                        }
                    }
                );
            } catch (Throwable t) {
                XposedBridge.log("WxRegion: skip UI hook " + name + ": " + t.getMessage());
            }
        }
    }

    private void hookRegionGetters(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] storageClasses = {
            "com.tencent.mm.storage.z",
            "com.tencent.mm.storage.Contact",
            "com.tencent.mm.contact.a",
        };
        String[] getters = {"getRegionCode", "getProvince", "getCity", "getCountryCode"};

        for (String cls : storageClasses) {
            for (String method : getters) {
                try {
                    XposedHelpers.findAndHookMethod(
                        cls,
                        lpparam.classLoader,
                        method,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                RegionMode mode = RegionPrefs.mode();
                                if (mode == null) {
                                    return;
                                }
                                if (mode == RegionMode.HIDDEN) {
                                    param.setResult("");
                                    return;
                                }
                                if ("getProvince".equals(method) || "getCity".equals(method)) {
                                    param.setResult("");
                                } else {
                                    param.setResult("CN");
                                }
                            }
                        }
                    );
                } catch (Throwable ignored) {
                    // not present in this build
                }
            }
        }

        XposedHelpers.findAndHookMethod(
            TextView.class,
            "setText",
            CharSequence.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    TextView tv = (TextView) param.thisObject;
                    if (tv.getContext() == null) {
                        return;
                    }
                    String ctxName = tv.getContext().getClass().getName();
                    if (!ctxName.contains("profile")) {
                        return;
                    }
                    RegionMode mode = RegionPrefs.mode();
                    if (mode == null) {
                        return;
                    }
                    Object arg = param.args[0];
                    if (arg == null) {
                        return;
                    }
                    String text = arg.toString();
                    if (!looksLikeRegionValue(text)) {
                        return;
                    }
                    param.args[0] = mode == RegionMode.HIDDEN ? "" : "中国大陆";
                }
            }
        );
    }

    private void applyUiMask(Activity activity) {
        RegionMode mode = RegionPrefs.mode();
        if (mode == null) {
            return;
        }
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        traverse(root, view -> {
            if (!(view instanceof TextView label)) {
                return;
            }
            String text = label.getText() != null ? label.getText().toString() : "";
            if (!"地区".equals(text) && !text.contains("Region")) {
                return;
            }
            View row = findRowContainer(label);
            TextView valueView = findSiblingValueText(label);
            if (mode == RegionMode.HIDDEN) {
                if (row != null) {
                    row.setVisibility(View.GONE);
                }
            } else {
                if (valueView != null) {
                    valueView.setText("中国大陆");
                    valueView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private interface ViewVisitor {
        void visit(View view);
    }

    private void traverse(View view, ViewVisitor visitor) {
        visitor.visit(view);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                traverse(group.getChildAt(i), visitor);
            }
        }
    }

    private View findRowContainer(TextView label) {
        View current = label;
        for (int i = 0; i < 4; i++) {
            if (!(current.getParent() instanceof View parent)) {
                break;
            }
            current = parent;
            if (current instanceof ViewGroup group && group.getChildCount() >= 2) {
                return current;
            }
        }
        return label.getParent() instanceof View parent ? parent : null;
    }

    private TextView findSiblingValueText(TextView label) {
        if (!(label.getParent() instanceof ViewGroup parent)) {
            return null;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView sibling && child != label) {
                return sibling;
            }
        }
        return null;
    }

    private boolean looksLikeRegionValue(String text) {
        if (text.isBlank() || "地区".equals(text) || text.length() > 24) {
            return false;
        }
        return text.contains("省")
            || text.contains("市")
            || text.contains("中国")
            || text.contains("大陆")
            || text.matches("[\\u4e00-\\u9fa5A-Za-z\\s]+");
    }
}
