import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.reader()?.use { load(it) }
}
val sncfApiKey = localProperties.getProperty("ontime.sncfApiKey").orEmpty()
require(sncfApiKey.none { it == '"' || it == '\\' }) { "ontime.sncfApiKey contains invalid characters" }

android {
    namespace = "fr.ontime.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.ontime.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Personal use only: a key embedded in an APK can be extracted.
        buildConfigField("String", "SNCF_API_KEY", "\"$sncfApiKey\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.json:json:20240303")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
