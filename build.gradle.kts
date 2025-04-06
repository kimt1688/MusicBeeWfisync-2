import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
// Existing plugins
//    id("com.android.application") version "8.9.1" apply false
//}

buildscript {
    //ext {
    //    agp_version = '8.9.1'
    //}
    //ext.agp_version = '8.9,1'
    //ext.kotlin_version = '2.1.10'
    extra.apply {
        set("agpversion", "8.9.1")
        set("kotlinversion", "2.1.10")
    }
    repositories {
        google()
        mavenCentral()
        
    }
    val agpversion: String by extra
    val kotlinversion: String by extra
    dependencies {
        classpath ("com.android.tools.build:gradle:$agpversion")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinversion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
