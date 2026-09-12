# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * implements dagger.hilt.internal.TestSingletonComponent { *; }
-keep class * extends javax.inject.Provider

# Health Connect
-keep class androidx.health.connect.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ObjectBox
-keep class io.objectbox.** { *; }

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# XmlPullParser duplicate class resolution
-dontwarn org.xmlpull.v1.**
-dontwarn net.sf.kxml.**

# PDFBox / EPubLib / Image Decoders
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.positiondev.epublib.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**
