# Reglas de ProGuard/R8 (la build release no minifica, pero se dejan por si se activa).

# Modelos serializados con Gson
-keep class com.example.githubclient.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keepattributes Exceptions
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
