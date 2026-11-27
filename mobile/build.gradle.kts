plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Два вида билда:
//  - dev  («мой»): креды-пресет из .env.dev зашиваются в BuildConfig, всё работает сразу;
//  - git  («чистый»): без пресета, пользователь вводит endpoint/токен/модели сам в настройках.
// .env.dev в gitignore — секреты в репозиторий не попадают.
val envDev = rootProject.file(".env.dev").let { f ->
    if (f.exists()) f.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
        .associate {
            val (k, v) = it.split("=", limit = 2)
            k.trim() to v.trim()
        }
    else emptyMap()
}

android {
    namespace = "com.witness.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.witness"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("git") {
            dimension = "distribution"
            buildConfigField("String", "WITNESS_ASR_URL", "\"\"")
            buildConfigField("String", "WITNESS_ASR_MODEL", "\"\"")
            buildConfigField("String", "WITNESS_LLM_URL", "\"\"")
            buildConfigField("String", "WITNESS_LLM_MODEL", "\"\"")
            buildConfigField("String", "WITNESS_API_KEY", "\"\"")
        }
        create("dev") {
            dimension = "distribution"
            buildConfigField("String", "WITNESS_ASR_URL", "\"${envDev["WITNESS_ASR_URL"] ?: ""}\"")
            buildConfigField("String", "WITNESS_ASR_MODEL", "\"${envDev["WITNESS_ASR_MODEL"] ?: ""}\"")
            buildConfigField("String", "WITNESS_LLM_URL", "\"${envDev["WITNESS_LLM_URL"] ?: ""}\"")
            buildConfigField("String", "WITNESS_LLM_MODEL", "\"${envDev["WITNESS_LLM_MODEL"] ?: ""}\"")
            buildConfigField("String", "WITNESS_API_KEY", "\"${envDev["WITNESS_API_KEY"] ?: ""}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.play.services.wear)
    implementation("androidx.media:media:1.7.0")
}
