import com.android.build.api.dsl.Packaging
import com.android.sdklib.AndroidVersion.VersionCodes.*

plugins {
    // Existing plugins
   id("com.android.application")
   id("kotlin-android")
   alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 36
    namespace = "kim.tkland.musicbeewifisync"
    ndkVersion = "29.0.14033849"

    defaultConfig {
        applicationId = "kim.tkland.musicbeewifisync"
        minSdk = 31
        versionCode = 149
        versionName = "3.0.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 35
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    buildFeatures {
        //viewBinding = true
        compose = true
    }
    kotlinOptions {
        jvmTarget = "19"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
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

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    /* implementation(libs.core) */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation (libs.androidx.compose.material3.material32)
    implementation (libs.androidx.material3.window.size.class1)
    implementation (libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.foundation)
    implementation(libs.litert.metadata)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.ui.graphics)
    implementation(libs.hilt.android)
    /*
    implementation(libs.rendering)
    debugImplementation(libs.androidx.ui.tooling)
    */
    implementation(libs.kotlinx.coroutines.core) // Or the latest version
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.material3)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.compose.material3) // Or the latest version

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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.litert.metadata)

    //testImplementation (libs.junit)
    //androidTestImplementation (libs.androidx.junit)
    //testImplementation (libs.androidx.espresso.core)
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
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
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation.layout)
    //androidTestImplementation (platform(libs.compose.bom))
    //androidTestImplementation(libs.androidx.ui.test.junit4)
    //debugImplementation(libs.androidx.ui.tooling)
    //debugImplementation(libs.androidx.ui.test.manifest)
}
repositories {
    mavenCentral()
}
