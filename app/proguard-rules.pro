# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# DevInfo keep rules (added when enabling R8/resource shrinking)
# ---------------------------------------------------------------------------

# DeviceInfoCollector reads android.os.SystemProperties via reflection
# (getMethod("get", String)). Keep it so R8 doesn't strip/rename it.
-keep class android.os.SystemProperties {
    java.lang.String get(java.lang.String);
}

# Preserve line numbers for readable crash stack traces, and keep the
# source-file attribute so traces still map back to files after minify.
-keepattributes SourceFile,LineNumberTable

# Kotlin metadata / coroutines are handled by AGP's bundled consumer rules,
# but keep enum values() used via InfoCategory.entries reflection paths.
-keepclassmembers enum com.fioiu8.devinfo.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
