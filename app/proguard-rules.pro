# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keep,includedescriptorclasses class com.iptv.player.**$$serializer { *; }
-keepclassmembers class com.iptv.player.** {
    *** Companion;
}
-keepclasseswithmembers class com.iptv.player.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepattributes Signature
-keepattributes Exceptions
