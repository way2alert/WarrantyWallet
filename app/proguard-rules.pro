# Proguard rules for Warranty Wallet

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep ML Kit text recognition
-keep class com.google.mlkit.vision.text.** { *; }

# Keep Firebase models and entities
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
