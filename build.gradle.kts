// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
// Existing plugins
//    id("com.android.application") version "8.9.1" apply false
//}
plugins {
    // Existing plugins
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    val agpversion by extra("9.3.0")
    val kotlinversion by extra("2.4.10")
    repositories {
        google()
        mavenCentral()
        
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:$agpversion")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinversion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
val debugDebuggable by extra(true)


allprojects {
    repositories {
        google()
        mavenCentral()
    }
    //tasks.withType(JavaCompile::class.java).configureEach {
    //    options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    //}
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
