// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "6.1.2"
}
//plugins {
    // Existing plugins
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.compose.compiler)
//}

android {
    compileSdk = 35
    namespace = "kim.tkland.musicbeewifisync"
    ndkVersion = "29.0.13113456"

    defaultConfig {
        applicationId = "kim.tkland.musicbeewifisync"
        minSdk = 31
        versionCode = 124
        versionName = "2.8.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 35
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled =  true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isJniDebuggable = false
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled =  false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isJniDebuggable = true
            // matchingFallbacks += listOf("")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "19"
    }
}

// Javaコンパイル時
tasks.withType<JavaCompile> {
    options.compilerArgs.plus("-Xlint:unchecked")
    options.compilerArgs.plus("-Xlint:deprecation")
}

// Kotlinコンパイル時
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xnon-local-break-continue"))
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

	implementation (libs.androidx.constraintlayout)
    implementation (libs.androidx.appcompat.resources)
    implementation (libs.material)
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)
    implementation (libs.androidx.ui.tooling)
    implementation (libs.androidx.ui.tooling.preview)
    implementation (libs.androidx.ui)
    implementation (libs.kotlin.stdlib)
    implementation (libs.kotlin.reflect)
    implementation (libs.androidx.leanback)
    testImplementation (libs.junit)
    testImplementation (libs.androidx.espresso.core)
    implementation (libs.kotlin.stdlib.jdk7)
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.core.ktx)
    implementation (libs.kotlin.stdlib.jdk7)
    implementation (libs.androidx.activity.ktx)
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.androidx.runtime)
    implementation (libs.kotlinx.coroutines.core.jvm)
    implementation (libs.androidx.activity.compose)
    implementation (libs.androidx.preference.ktx)
    implementation (libs.androidx.documentfile)
    implementation (libs.kotlinx.coroutines.android)
    implementation (platform(libs.compose.bom))
}
repositories {
//    mavenCentral()
    google()
    mavenCentral {
        metadataSources {
            mavenPom()
            gradleMetadata()
        }
    }
}
