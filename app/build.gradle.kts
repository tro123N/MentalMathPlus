import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.reproducible.builds)
}
hilt {
    enableAggregatingTask = false
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.helddertierwelt.mentalmath"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.helddertierwelt.mentalmath"
        minSdk = 24
        targetSdk = 36
        versionCode = 30
        versionName = "30"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        listOf("dev", "foss", "play").forEach { flavor ->
            val prefix = flavor.uppercase()
            val storeFile = providers.gradleProperty("${prefix}_STORE_FILE").orNull
            val storePassword = providers.gradleProperty("${prefix}_STORE_PASSWORD").orNull
            val keyAlias = providers.gradleProperty("${prefix}_KEY_ALIAS").orNull
            val keyPassword = providers.gradleProperty("${prefix}_KEY_PASSWORD").orNull

            if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
                create("${flavor}Release") {
                    this.storeFile = file(storeFile)
                    this.storePassword = storePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("dev") {
            dimension = "distribution"
            signingConfig = signingConfigs.findByName("devRelease")
        }
        create("foss") {
            dimension = "distribution"
            signingConfig = signingConfigs.findByName("fossRelease")
        }
        create("play") {
            dimension = "distribution"
            signingConfig = signingConfigs.findByName("playRelease")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        resources {
            excludes += "assets/dexopt/baseline.prof"
            excludes += "assets/dexopt/baseline.profm"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.matching { it.name.contains("Profile", ignoreCase = true) }.configureEach {
    enabled = false
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xdeterministic")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.protobuf.java)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java")
            }
        }
    }
}