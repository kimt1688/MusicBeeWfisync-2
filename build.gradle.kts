buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:8.14.2")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
// val debugDebuggable by extra(true)


allprojects {
    repositories {
        google()
        mavenCentral {
            metadataSources {
                mavenPom()
                gradleMetadata()
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
