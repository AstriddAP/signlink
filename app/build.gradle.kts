import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.signlink"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""
    val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY") ?: ""
    val dbPassphrase: String = localProperties.getProperty("DB_PASSPHRASE") ?: "clave_segura_signlink_2024"

    defaultConfig {
        applicationId = "com.signlink"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        
        // Esto crea las llaves de forma segura para usar en el código
        buildConfigField("String", "DB_PASSPHRASE", "\"$dbPassphrase\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/common.proto"
            excludes += "google/protobuf/*.proto"
            excludes += "**/*.proto"
        }
    }
}

configurations {
    all {
        exclude(group = "com.google.guava", module = "listenablefuture")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
    }
}

dependencies {
    implementation("com.google.api.grpc:proto-google-common-protos:2.49.0")
    implementation("com.google.protobuf:protobuf-java:3.25.5")
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.google.cloud.speech) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation(libs.google.auth.library)
    implementation(libs.grpc.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.sqlcipher)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    implementation(libs.google.generativeai)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.obj.detection)
    implementation(libs.mlkit.img.labeling)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.glide)
    implementation(libs.lottie)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.concurrent.futures)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
