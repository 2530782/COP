package com.poligono.cop.data.remote

import com.poligono.cop.data.dto.CitaDto
import com.poligono.cop.data.dto.LoginRequest
import com.poligono.cop.data.dto.ProfesionalDto
import com.poligono.cop.data.dto.ServicioDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Aquí están TODAS las peticiones HTTP al backend COP.
 * Cada función corresponde a una ruta del archivo Routing.kt del backend:
 *
 *   POST /login          -> login()
 *   GET  /servicios      -> servicios()
 *   GET  /profesionales  -> profesionales()
 *   GET  /citas          -> citas()
 */
class CopApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    /** Regresa la respuesta "cruda" para poder leer el código de estado (200, 401, 423...). */
    suspend fun login(request: LoginRequest): HttpResponse =
        client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)   // avisamos que mandamos JSON
            setBody(request)                            // el serializador convierte request a JSON
        }

    // .body<T>() usa el serializador para convertir el JSON de respuesta en objetos Kotlin
    suspend fun servicios(): List<ServicioDto> =
        client.get("$baseUrl/servicios").body()

    suspend fun profesionales(): List<ProfesionalDto> =
        client.get("$baseUrl/profesionales").body()

    /** Ejemplo de URL final: http://10.0.2.2:8080/citas?paciente=ana */
    suspend fun citas(paciente: String?, profesional: String?): List<CitaDto> =
        client.get("$baseUrl/citas") {
            if (paciente != null) parameter("paciente", paciente)
            if (profesional != null) parameter("profesional", profesional)
        }.body()
}
