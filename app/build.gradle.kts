plugins {
    id("com.android.application")
}

android {
    compileSdk = 34
    namespace = "org.ameelio.pdfviewer"

    defaultConfig {
        applicationId = "org.ameelio.pdfviewer"
        minSdk = 34
        targetSdk = 34
        versionCode = 4
        versionName = "1.1.2"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
    // Robolectric's transitive guava/bouncycastle are older than the patched versions in Dependabot
    // advisories; test classpath only, nothing here ships in the APK.
    constraints {
        testImplementation("com.google.guava:guava:33.7.2-jre")
        testImplementation("org.bouncycastle:bcprov-jdk18on:1.85")
    }

    // Android instrumentation tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
}
