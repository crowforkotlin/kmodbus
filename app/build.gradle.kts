import com.android.build.gradle.internal.cxx.configure.abiOf
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.tasks.NativeBuildSystem

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(app.plugins.android.application)
    alias(app.plugins.android.kotlin)
}

android {
    namespace = "com.crow.kmodbus"

    compileSdk = 34

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.crow.kmodbus"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging.resources {
        excludes.add("META-INF/INDEX.LIST")
        excludes.add("META-INF/io.netty.versions.properties")
        excludes.add("META-INF/gradle/incremental.annotation.processors")
        excludes.add("META-INF/LICENSE")
        excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {
    // 引入App下libs文件下的所有Jar包
//    implementation(fileTree("dir" to "libs", "include" to "*.aar"))
//    implementation("com.kotlincrow.android.component:KModbus:1.0")
//    implementation(file("/app/src/libs/KModbus-1.1.aar"))O
//    implementation(files("libs/KModbus-1.1.aar"))

    implementation(project(":lib_modbus"))
    implementation(app.androidx.core.ktx)
    implementation(app.androidx.appcompat)
    implementation(app.androidx.material)
    implementation(app.androidx.constraintlayout)
//    implementation(libs.gson)
}