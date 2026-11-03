package com.poligono.cop

import com.poligono.cop.data.JsonDataSource
import com.poligono.cop.services.AuthService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * PUNTO DE ENTRADA del backend.
 * Levanta un servidor Netty en el puerto 8080.
 * host = "0.0.0.0" -> acepta conexiones de cualquier dispositivo
 * (necesario para que el emulador o un celular lleguen a la PC).
 */
fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

/**
 * "Módulo" de la aplicación: aquí se instalan los plugins y las rutas, en orden.
 * Cada configure...() está en su propio archivo (igual que lo genera el wizard de Ktor).
 */
fun Application.module() {
    // 1) Cargamos los archivos JSON de resources/data una sola vez al arrancar
    val datos = JsonDataSource()
    val auth = AuthService(datos.usuarios)

    // 2) Plugins
    configureSerialization()   // Serialization.kt
    configureHTTP()            // HTTP.kt
    configureMonitoring()      // HTTP.kt

    // 3) Rutas (endpoints)
    configureRouting(datos, auth)   // Routing.kt
}
