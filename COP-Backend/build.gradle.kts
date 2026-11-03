plugins {
    alias(libs.plugins.kotlinJvm)            // Kotlin para la JVM (servidor)
    alias(libs.plugins.kotlinSerialization)  // para que @Serializable funcione
    application                              // agrega la tarea "run" para levantar el servidor
}

group = "com.poligono.cop"
version = "1.0.0"

application {
    // Clase que contiene fun main(): el archivo Application.kt se compila como "ApplicationKt"
    mainClass.set("com.poligono.cop.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)                // núcleo de Ktor Server
    implementation(libs.ktor.server.netty)               // motor que atiende las peticiones HTTP
    implementation(libs.ktor.server.contentNegotiation)  // negociación de contenido (JSON)
    implementation(libs.ktor.serialization.kotlinxJson)  // SERIALIZADOR kotlinx.serialization
    implementation(libs.ktor.server.cors)                // permite llamadas desde la versión web
    implementation(libs.ktor.server.callLogging)         // bitácora de peticiones (COP-RS-06)
    implementation(libs.ktor.server.statusPages)         // respuestas de error ordenadas
    implementation(libs.logback.classic)                 // imprime los logs en consola
    implementation(libs.ktor.server.auth)                // sesión con token (COP-RC-04)
    implementation(libs.ktor.server.authJwt)             // token JWT con el rol del usuario
    implementation(libs.h2)                              // base de datos local (archivo datos/cop.mv.db)
    implementation(libs.postgresql)                      // base de datos en Render
    implementation(libs.openpdf)                         // exportar agenda en PDF (COP-RI-07)

    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
}
