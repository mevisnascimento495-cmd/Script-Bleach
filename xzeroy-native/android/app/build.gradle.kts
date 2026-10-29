plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
    namespace = "dev.xzeroy.app"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets.getByName("main").java.srcDir("../../engine/protohax/src/main/java")
    buildFeatures { buildConfig = true }
    val microsoftClientId = providers.gradleProperty("microsoftClientId")
        .orElse(providers.environmentVariable("MICROSOFT_CLIENT_ID"))
        .getOrElse("")
    defaultConfig {
        applicationId = "dev.xzeroy.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"
        buildConfigField("String", "MICROSOFT_CLIENT_ID", "\"${microsoftClientId}\"")
    }
}


dependencies {
    // Compile the pinned, GPL-licensed ProtoHax source directly. The published 1.4.0
    // POM exists but its JAR is absent from the configured package repository.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta1-SNAPSHOT") {
        exclude(group = "com.nukkitx", module = "natives")
    }
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("coelho.msftauth:minecraft-msftauth:2.1.6") {
        exclude(group = "com.squareup.okhttp3")
    }
    implementation("io.github._0x9fff00.leveldb:leveldb:0.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.11.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
}
