# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimize and shrink code (use AGP defaults; avoid aggressive optimizations that may break MediaPipe tracing)
-verbose
# Disable code optimizations globally to preserve stack/caller information used by androidx.tracing/MediaPipe
-dontoptimize


# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.activity.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# TensorFlow / MediaPipe
-keep class org.tensorflow.** { *; }
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.tasks.** { *; }
-keep class com.google.mediapipe.tasks.vision.core.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }

# Hindari obfuscation pada model loader atau file .tflite
-keepclassmembers class * {
    public <init>(...);
}

# Jika menggunakan kotlin coroutines / flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep UI preview & runtime errors debug info (opsional)
-keepattributes *Annotation*, InnerClasses

# Keep the application class
-keep public class ai.pilahsampah.** { *; }

# Keep all native methods and their classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the support library classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep MediaPipe classes (package-wide to avoid unresolved specific classes)
-keep class com.google.mediapipe.** { *; }

# Keep the entry points
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Keep any classes referenced from XML layouts
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Preserve the line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, hide the original source file name
-renamesourcefileattribute SourceFile 

-dontwarn com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8

# --- Protobuf & MediaPipe keep rules (release build stability) ---
# Protobuf (javalite) used by MediaPipe
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**

# Preserve tracing stack info used by MediaPipe
-keepattributes EnclosingMethod,Signature,InnerClasses,SourceFile,LineNumberTable

# AndroidX tracing (MediaPipe depends on it)
-keep class androidx.tracing.** { *; }
-dontwarn androidx.tracing.**

# Note: Do not reference obfuscated short packages (q0.*, z1.*) in keep rules.
# We preserve tracing via androidx.tracing.* and keepattributes above.

# MediaPipe core (covered by package-wide keep above)
# -keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
# --- End Protobuf & MediaPipe keep rules ---