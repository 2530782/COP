import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Aplicación Web (Kotlin/Wasm)
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // Nombre de los archivos generados: webApp.js y webApp.wasm
        outputModuleName.set("webApp")

        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }

        // Genera la app ejecutable en el navegador
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            // Pantallas, API y funciones de plataforma
            implementation(project(":shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
    }
}