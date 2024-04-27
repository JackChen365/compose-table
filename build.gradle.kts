buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application").version("8.3.2") apply false
    id("com.android.library").version("8.3.2") apply false
    id("org.jetbrains.kotlin.android").version("1.9.23") apply false
    id("com.google.devtools.ksp").version("1.7.0-1.0.6") apply false
}