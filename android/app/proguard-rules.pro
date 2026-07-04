-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class pro.xservis.client.**$$serializer { *; }
-keepclassmembers class pro.xservis.client.** {
    *** Companion;
}
-keepclasseswithmembers class pro.xservis.client.** {
    kotlinx.serialization.KSerializer serializer(...);
}
