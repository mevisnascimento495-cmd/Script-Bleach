plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "dev.xzeroy.app"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { buildConfig = true }
    val microsoftClientId = providers.gradleProperty("microsoftClientId")
        .orElse(providers.environmentVariable("MICROSOFT_CLIENT_ID"))
        .getOrElse("")
    defaultConfig {
        applicationId = "dev.xzeroy.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
        buildConfigField("String", "MICROSOFT_CLIENT_ID", "\"${microsoftClientId}\"")
    }
}
