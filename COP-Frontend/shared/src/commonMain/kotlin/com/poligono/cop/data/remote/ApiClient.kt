package com.poligono.cop.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Configuración ÚNICA del cliente HTTP de Ktor (Ktor Client).
 * "object" = solo existe una instancia en toda la app (singleton),
 * así no se crea un cliente nuevo en cada petición.
 *
 * No se indica el motor (OkHttp / Darwin): Ktor lo toma solo de las dependencias
 * de cada plataforma (androidMain usa OkHttp, iosMain usa Darwin).
 */
object ApiClient {

    val httpClient: HttpClient = HttpClient {

        // false = Ktor NO lanza excepción con 401/423; nosotros revisamos el código de estado.
        expectSuccess = false

        // Plugin del SERIALIZADOR: convierte automáticamente JSON <-> clases @Serializable
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true   // si el backend agrega campos nuevos, no truena
                    isLenient = true
                }
            )
        }

        // Si el servidor no contesta, no nos quedamos esperando para siempre
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
    }
}
