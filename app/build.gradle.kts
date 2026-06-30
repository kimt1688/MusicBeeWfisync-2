//import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    // Existing plugins
   id("com.android.application")
   alias(libs.plugins.compose.compiler)
}

android {
    37.also { compileSdk = it }
    namespace = "kim.tkland.musicbeewifisync"
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "kim.tkland.musicbeewifisync"
        minSdk = 31
        versionCode = 167
        versionName = "3.1.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 36
    }

    buildTypes {
        getByName("release") {
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        //viewBinding = true
        compose = true
    }
    // compileSdkExtension = 18
}

/*
composeCompiler {
    featureFlags = setOf(
        ComposeFeatureFlag.StrongSkipping
    )
}
*/

// Javaコンパイル時
tasks.withType<JavaCompile> {
    options.compilerArgs.plus("-Xlint:unchecked")
    options.compilerArgs.plus("-Xlint:deprecation")
}

// Kotlinコンパイル時
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // Keep your existing args
        freeCompilerArgs.add("-Xnon-local-break-continue")

        // ADD THIS LINE to disable Light Tree and use the standard PSI parser
        freeCompilerArgs.add("-Xuse-fir-lt=false")
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
    // implementation(libs.litert.metadata)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.ui.graphics)
    implementation(libs.hilt.android)

    implementation(libs.kotlinx.coroutines.android)
    ///implementation(libs.androidx.compose.material3)
    implementation(libs.accompanist.systemuicontroller)

    implementation (libs.androidx.constraintlayout)
    implementation (libs.androidx.appcompat.resources)
    implementation (libs.material)
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)
    implementation (libs.androidx.ui.tooling)
    implementation (libs.kotlin.reflect)
    implementation (libs.androidx.leanback)
    implementation(project(":app:poweramp_api_lib"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.navigation3.runtime)

    //testImplementation (libs.junit)
    //androidTestImplementation (libs.androidx.junit)
    //testImplementation (libs.androidx.espresso.core)
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.kotlin.stdlib.jdk7)
    implementation (libs.androidx.activity.ktx)
    implementation (libs.androidx.fragment.ktx)
    //implementation (libs.fragment.ktx)
    implementation (libs.androidx.runtime)
    implementation (libs.kotlinx.coroutines.core.jvm)
    implementation (libs.androidx.activity.compose)
    implementation (libs.androidx.preference.ktx)
    implementation (libs.androidx.documentfile)
    implementation (platform(libs.compose.bom))
    ///implementation(libs.androidx.compose.runtime)
    //androidTestImplementation (platform(libs.compose.bom))
    //androidTestImplementation(libs.androidx.ui.test.junit4)
    //debugImplementation(libs.androidx.ui.tooling)
    //debugImplementation(libs.androidx.ui.test.manifest)
}
repositories {
    mavenCentral()
}
