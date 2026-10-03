# Add project specific ProGuard rules here.
# Room Database Entities & Models
-keep class com.example.data.local.** { *; }
-keep class com.example.data.model.** { *; }
-keepattributes *Annotation*,Signature,EnclosingMethod

# Keep Room generated implementations
-keeppackagename androidx.room.persistence.**
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
