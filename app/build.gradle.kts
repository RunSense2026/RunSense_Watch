plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bmb.runsensewear"
    compileSdk = 36

    defaultConfig {
        // 폰 앱(RunSense_FrontEnd, com.runsense)과 같은 applicationId 여야
        // Wearable Data Layer 가 두 앱을 한 쌍으로 묶어 메시지를 전달합니다.
        applicationId = "com.runsense"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // 서버 주소는 BuildConfig로 넣어 두어 스테이징/운영 전환이 쉽도록 합니다.
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"https://runsense-backend-server-9he7.onrender.com/\""
        )
        buildConfigField(
            "String",
            "AI_BASE_URL",
            "\"https://runsense-ai-server.onrender.com/\""
        )
    }

    // 폰 앱과 같은 서명 키(android/app/debug.keystore 복사본). Data Layer 는 같은 서명을 요구합니다.
    signingConfigs {
        create("shared") {
            storeFile = file("runsense-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            signingConfig = signingConfigs.getByName("shared")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.health.services.client)
    implementation(libs.wear.compose.navigation)

    // 상태 홀더 / 포그라운드 서비스
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.wear.ongoing)

    // 코루틴 (Play Services Task / Guava ListenableFuture await)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
    implementation(libs.coroutines.guava)

    // 네트워크 (런센스 백엔드 / AI 코칭 서버)
    implementation(libs.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
