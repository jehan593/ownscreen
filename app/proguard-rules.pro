# OwnScreen is not shipped minified by default (see build.gradle.kts).
# Rules below apply if isMinifyEnabled is switched on for release builds.

-keep class com.ownscreen.app.data.db.entity.** { *; }
