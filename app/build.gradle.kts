import com.android.sdklib.AndroidVersion.VersionCodes.*

plugins {
    // Existing plugins
   id("com.android.application")
   //id("kotlin-android")
   alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 37
    namespace = "kim.tkland.musicbeewifisync"
    ndkVersion = "29.0.13113456"

    defaultConfig {
        applicationId = "kim.tkland.musicbeewifisync"
        minSdk = 31
        versionCode = 175
        versionName = "2.8.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 37
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled =  true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled =  false
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
    //kotlinOptions {
    //    jvmTarget = "19"
    //}
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
    implementation(project(":app:poweramp_api_lib"))
    implementation(libs.androidx.activity)
    implementation (libs.kotlin.stdlib.jdk7)
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.activity.ktx)
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.androidx.runtime)
    implementation (libs.kotlinx.coroutines.core.jvm)
    implementation (libs.androidx.activity.compose)
    implementation (libs.androidx.preference.ktx)
    implementation (libs.androidx.documentfile)
    implementation (libs.kotlinx.coroutines.android)

}
repositories {
    mavenCentral()
}
