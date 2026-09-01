# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ir.sabztime.app.** {
    *** Companion;
}
-keepclasseswithmembers class ir.sabztime.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
