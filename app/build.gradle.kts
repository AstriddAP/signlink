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
    val fcmServerKey: String = localProperties.getProperty("FCM_SERVER_KEY") ?: ""

    val versionPropsFile = project.file("version.properties")
    var currentVersionCode = 8
    if (versionPropsFile.exists()) {
        try {
            val versionProps = Properties()
            versionProps.load(versionPropsFile.inputStream())
            currentVersionCode = (versionProps.getProperty("VERSION_CODE") ?: "8").toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        try {
            val versionProps = Properties()
            versionProps.setProperty("VERSION_CODE", "8")
            versionPropsFile.parentFile.mkdirs()
            versionProps.store(versionPropsFile.outputStream(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isRelease = gradle.startParameter.taskNames.any { 
        it.contains("Release", ignoreCase = true) || it.contains("bundle", ignoreCase = true) 
    }
    if (isRelease) {
        currentVersionCode += 1
        try {
            val versionProps = Properties()
            versionProps.setProperty("VERSION_CODE", currentVersionCode.toString())
            versionProps.store(versionPropsFile.outputStream(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    defaultConfig {
        applicationId = "com.talkyapp.pe"
        minSdk = 28
        targetSdk = 35
        versionCode = currentVersionCode
        versionName = "1.6.$currentVersionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "FCM_SERVER_KEY", "\"$fcmServerKey\"")
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

            // Resolvemos conflictos de duplicados eligiendo el primero encontrado
            pickFirsts += "com/google/api/Advice*"
            pickFirsts += "com/google/api/LogConfig*"
            pickFirsts += "com/google/api/SystemParameter*"
            pickFirsts += "com/google/rpc/Status*"
            pickFirsts += "com/google/rpc/Code*"
            pickFirsts += "google/protobuf/*.proto"
            pickFirsts += "META-INF/provisions.gradle"
            
            // Muy importante para el error de GeneratedMessage y tipos comunes:
            pickFirsts += "com/google/protobuf/**"
            pickFirsts += "com/google/type/**"
            pickFirsts += "com/google/api/**"
            pickFirsts += "com/google/rpc/**"
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Forzamos la versión LITE para que sea compatible con Firebase y Android
        force("com.google.protobuf:protobuf-javalite:3.25.5")
        force("com.google.guava:guava:33.3.1-android")
    }
    // Excluimos la versión pesada de Java que causa el VerifyError
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
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

    // Usamos la versión LITE (obligatorio para Android/Firebase)
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")
    
    // Necesario para CameraX al no contar con la librería completa de Guava de forma automática
    implementation("com.google.guava:guava:33.3.1-android")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    implementation(libs.google.generativeai)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.obj.detection)
    implementation(libs.mlkit.img.labeling)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)

    implementation(libs.androidx.work.runtime.ktx)

    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation(libs.glide)
    implementation(libs.lottie)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.concurrent.futures)

    implementation(libs.retrofit.main)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.logging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
