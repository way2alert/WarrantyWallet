# Proguard rules for Warranty Wallet

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.warrantywallet.app.core.database.dao.** { *; }
-keep class com.warrantywallet.app.core.database.entity.** { *; }

# Keep Domain & UI Models
-keep class com.warrantywallet.app.core.model.** { *; }

# Keep ML Kit text recognition
-keep class com.google.mlkit.vision.text.** { *; }

# Keep Firebase models and entities
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Keep Google Play Billing
-keep class com.android.billingclient.api.** { *; }

# Keep Kotlinx Coroutines and Serialization
-dontwarn kotlinx.coroutines.**
-keepattributes Signature, InnerClasses, EnclosingMethod
