package com.poligono.cop

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/**
 * SERIALIZADOR del servidor.
 * ContentNegotiation + kotlinx.serialization hacen dos cosas automáticamente:
 *  - call.receive<LoginRequest>()  -> convierte el JSON que llega en un objeto Kotlin
 *  - call.respond(objeto)          -> convierte el objeto Kotlin en JSON de respuesta
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true         // JSON con saltos de línea (más fácil de leer al probar)
                ignoreUnknownKeys = true   // si llegan campos de más, no truena
                encodeDefaults = true      // manda TODOS los campos, aunque tengan valor por defecto
            }
        )
    }
}
