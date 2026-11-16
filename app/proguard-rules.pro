# JobHunter AI Proguard Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room entities
-keep class com.ghdinteractivestudio.jobhunter.data.local.** { *; }

# Keep JavaMail classes
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class java.awt.** { *; }
-dontwarn java.awt.**
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**

# Keep Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
