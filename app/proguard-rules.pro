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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Fragments may be refrenced in xml file or used for page restoration
-keep class * extends androidx.fragment.app.Fragment
-keepclassmembers class * extends androidx.fragment.app.Fragment {
 public <init>();
}

-keep class * extends com.madness.collision.util.TaggedFragment
-keepclassmembers class * extends com.madness.collision.util.TaggedFragment {
 public <init>();
}

# Page class uses Reflection to create fragment instance
# used with preference fragments, which extends PreferenceFragmentCompat
-keep class * extends androidx.preference.PreferenceFragmentCompat
-keepclassmembers class * extends androidx.preference.PreferenceFragmentCompat {
 public <init>();
}

# Navigation
#-keep class androidx.navigation.fragment.NavHostFragment
# This is refrenced in nav graph file
-keep class com.madness.collision.util.TypedNavArg

# Keep dynamic feature module classes
# com.madness.collision.unit.Unit extends Fragment
#-keep class * extends com.madness.collision.unit.Unit
-keep class * extends com.madness.collision.unit.Bridge { *; }

# Kotlin reflect
# T::class.createInstance() in com.madness.collision.util.Page seems to be needing this to work
-dontwarn kotlin.reflect.jvm.internal.**
-keep class kotlin.reflect.jvm.internal.** { *; }

# Xiaomi push detection
-keep class com.xiaomi.mipush.sdk.v {
 void d(...);
}
# keep the class and specified members from being renamed only
# classes in the sdk are obfuscated already
# those that are not are APIs and should be kept
-keepnames class com.xiaomi.** { *; }

# Bottom sheet background color
-keep class com.google.android.material.bottomsheet.BottomSheetBehavior {
 com.google.android.material.shape.MaterialShapeDrawable getMaterialShapeDrawable(...);
}

# For R8 full mode: AppDao dynamic proxy
-keep interface com.madness.collision.unit.api_viewing.database.AppDao

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
#-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
#-keep class com.google.gson.examples.android.model.** { *; }

##---------------End: proguard configuration for Gson  ----------
