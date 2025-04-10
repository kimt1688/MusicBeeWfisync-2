plugins {
    // Existing plugins
   id("com.android.application")
   id("kotlin-android")
}
android {
    compileSdk = 35
    namespace = "kim.tkland.musicbeewifisync"
    ndkVersion = "27.0.11902837 rc2"

    defaultConfig {
        applicationId = "kim.tkland.musicbeewifisync"
        minSdk = 31
        versionCode = 122
        versionName = "2.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        targetSdk = 35
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled =  false
            // proguardFiles = getDefaultProguardFile["proguard-android-optimize.txt", "proguard-rules.pro"]
            ndk.debugSymbolLevel = "SYMBOL_TABLE"
        }
        getByName("debug") {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    buildFeatures {
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    kotlinOptions {
        freeCompilerArgs = listOf("-Xnon-local-break-continue")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

	implementation ("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation ("androidx.appcompat:appcompat-resources:1.7.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation ("org.chromium.net:cronet-embedded:119.6045.31")
    implementation ("androidx.compose.ui:ui-tooling:1.7.8")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation ("androidx.compose.ui:ui:1.7.8")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    implementation ("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    implementation ("androidx.leanback:leanback:1.0.0")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.2.1")
    testImplementation ("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.10")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation ("androidx.core:core-ktx:1.15.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.10")
    implementation ("androidx.activity:activity-ktx:1.10.1")
    implementation ("androidx.fragment:fragment-ktx:1.8.6")
    implementation ("androidx.compose.runtime:runtime:1.7.8")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
    implementation ("androidx.activity:activity-compose:1.10.1")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("androidx.documentfile:documentfile:1.0.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation (platform("androidx.compose:compose-bom:2025.03.01"))
    androidTestImplementation (platform("androidx.compose:compose-bom:2025.03.01"))
}
repositories {
    mavenCentral()
}
