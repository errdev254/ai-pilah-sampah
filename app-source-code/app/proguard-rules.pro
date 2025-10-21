# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimize and shrink code
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*


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

# Keep MediaPipe classes
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

# --- Protobuf & MediaPipe reflection keep rules (added for release build crash fix) ---
# Keep all fields and methods for protobuf classes (required for reflection)
-keep class com.google.protobuf.** { *; }
-keepclassmembers class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessage {
    <fields>;
    <methods>;
}
# Keep typeUrl_ and other fields for MediaPipe/Protobuf
-keepclassmembers class com.google.protobuf.h {
    <fields>;
    <methods>;
}
# --- End Protobuf & MediaPipe keep rules ---