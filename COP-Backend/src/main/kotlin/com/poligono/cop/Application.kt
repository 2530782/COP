package com.poligono.cop

import com.poligono.cop.data.BaseDatos
import com.poligono.cop.data.Bitacora
import com.poligono.cop.services.AuthService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * PUNTO DE ENTRADA del backend.
 * Levanta un servidor Netty en el puerto 8080 (o el que indique la variable PORT en Render/Railway).
 * host = "0.0.0.0" -> acepta conexiones de cualquier dispositivo
 * (necesario para que el emulador o un celular lleguen a la PC).
 */
fun main() {
    embeddedServer(
        Netty,
        port = Configuracion.puerto,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

/**
 * "Módulo" de la aplicación: aquí se instalan los plugins y las rutas, en orden.
 * Cada configure...() está en su propio archivo (igual que lo genera el wizard de Ktor).
 *
 * [urlBaseDatosPruebas]: las pruebas usan una base en memoria.
 */
fun Application.module(urlBaseDatosPruebas: String? = null) {
    // 1) Base de datos: crea las tablas y, la primera vez, copia los JSON de resources/data
    BaseDatos.iniciar(urlBaseDatosPruebas)
    val auth = AuthService()

    // 2) Plugins
    configureSerialization()   // Serialization.kt
    configureHTTP()            // HTTP.kt
    configureMonitoring()      // HTTP.kt
    configureSeguridad()       // Seguridad.kt

    // 3) Rutas (endpoints)
    configureRouting(auth)     // Routing.kt

    Bitacora.info("Servidor COP iniciado en el puerto ${Configuracion.puerto}")
}
