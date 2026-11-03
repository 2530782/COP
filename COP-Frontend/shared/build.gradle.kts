import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Necesario para que @Serializable genere el código que convierte JSON <-> clases
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Destinos iOS (COP-RE-02: versión futura para iOS)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // Destino Android
    android {
        namespace = "com.poligono.cop.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        // Código que solo se compila para Android
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            // Motor HTTP de Ktor para Android
            implementation(libs.ktor.client.okhttp)
            // Permisos (GPS, notificaciones) y botón "atrás"
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            // Mapa libre de OpenStreetMap (no requiere clave ni cuenta de pago)
            implementation(libs.osmdroid.android)
            // Recordatorios programados y revisión de avisos en segundo plano
            implementation(libs.androidx.work.runtime)
        }
        // Código que solo se compila para iOS
        iosMain.dependencies {
            // Motor HTTP de Ktor para iOS
            implementation(libs.ktor.client.darwin)
        }
        // Código compartido por TODAS las plataformas
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Ktor Client: peticiones HTTP + conversión JSON (serializador)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
