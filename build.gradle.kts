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
    id("com.android.application").version("7.2.1") apply false
    id("com.android.library").version("7.2.1") apply false
    id("org.jetbrains.kotlin.android").version("1.6.21") apply false
    id("com.google.devtools.ksp").version("1.7.0-1.0.6") apply false
    id("com.github.ben-manes.versions").version("0.41.0")
    id("nl.littlerobots.version-catalog-update").version("0.6.1")
}
subprojects {
    repositories {
        google()
        mavenCentral()
    }
}
apply("${project.rootDir}/buildscripts/toml-updater-config.gradle")