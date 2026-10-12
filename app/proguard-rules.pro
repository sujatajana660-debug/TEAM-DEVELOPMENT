# ProGuard & R8 configuration for Stack Ball Release APK

# Keep line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Game Models and Data Classes
-keep class com.example.model.** { *; }
-keep class com.example.engine.** { *; }
-keep class com.example.audio.** { *; }

# Keep Compose & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Audio & Media Playback
-dontwarn android.media.**
