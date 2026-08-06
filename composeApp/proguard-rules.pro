# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Accessibility service
-keep class com.wxregion.platform.WeChatRegionAccessibilityService { *; }

# Shared / engine
-keep class com.wxregion.engine.** { *; }
-keep class com.wxregion.config.** { *; }
-keep class com.wxregion.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.wxregion.**$$serializer { *; }
-keepclassmembers class com.wxregion.** {
    *** Companion;
}
-keepclasseswithmembers class com.wxregion.** {
    kotlinx.serialization.KSerializer serializer(...);
}
