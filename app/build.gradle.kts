
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)

    id("org.gradlex.reproducible-builds") version "1.1"
}
hilt {
    enableAggregatingTask = false
}

android {
    namespace = "com.helddertierwelt.mentalmath"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.helddertierwelt.mentalmath"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "14"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    signingConfigs {
        create("fossRelease") {
            storeFile = file(project.property("FOSS_STORE_FILE") as String)
            storePassword = project.property("FOSS_STORE_PASSWORD") as String
            keyAlias = project.property("FOSS_KEY_ALIAS") as String
            keyPassword = project.property("FOSS_KEY_PASSWORD") as String
        }
        create("playRelease") {
            storeFile = file(project.property("PLAY_STORE_FILE") as String)
            storePassword = project.property("PLAY_STORE_PASSWORD") as String
            keyAlias = project.property("PLAY_KEY_ALIAS") as String
            keyPassword = project.property("PLAY_KEY_PASSWORD") as String
        }
        create("devRelease") {
            storeFile = file(project.property("DEV_STORE_FILE") as String)
            storePassword = project.property("DEV_STORE_PASSWORD") as String
            keyAlias = project.property("DEV_KEY_ALIAS") as String
            keyPassword = project.property("DEV_KEY_PASSWORD") as String
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            signingConfig = null
        }
        create("foss") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("fossRelease")
        }
        create("play") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("playRelease")
        }
        create("dev") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("devRelease")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs.keepDebugSymbols += "**/*.so"
    }
    packaging {
        resources {
            excludes += "assets/dexopt/baseline.prof"
            excludes += "assets/dexopt/baseline.profm"
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

}

tasks.matching { it.name.contains("Profile", ignoreCase = true) }.configureEach {
    enabled = false
}
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.withType<Zip>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(listOf("-Xdeterministic", "-opt-in=kotlin.RequiresOptIn"))
    }
}

dependencies {

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.protobuf.java)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.hilt.lifecycle.viewmodel)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.0"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java")
            }
        }
    }
}