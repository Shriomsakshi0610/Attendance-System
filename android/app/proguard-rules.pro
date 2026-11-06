# Renaissance Teacher Proguard Rules
-keep class com.renaissance.teacher.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
