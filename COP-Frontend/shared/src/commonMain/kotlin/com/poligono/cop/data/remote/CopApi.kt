package com.poligono.cop.data.remote

import com.poligono.cop.data.dto.CambioEstadoRequest
import com.poligono.cop.data.dto.CitaManualRequest
import com.poligono.cop.data.dto.CitaRequest
import com.poligono.cop.data.dto.HorarioRequest
import com.poligono.cop.data.dto.LoginRequest
import com.poligono.cop.data.dto.ProfesionalRequest
import com.poligono.cop.data.dto.RecuperarRequest
import com.poligono.cop.data.dto.RegistroRequest
import com.poligono.cop.data.dto.ReporteRequest
import com.poligono.cop.data.dto.ReprogramarRequest
import com.poligono.cop.data.dto.RestablecerRequest
import com.poligono.cop.data.dto.ServicioDto
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter

/**
 * Aquí están TODAS las peticiones HTTP al backend COP.
 * Cada función corresponde a una ruta de COP-Backend (Routing.kt y carpeta rutas/).
 *
 * Todas regresan la respuesta "cruda" (HttpResponse) para que el repositorio
 * revise el código de estado (200, 401, 409...) y lea el JSON con .body<T>().
 *
 * [token] da el token de la sesión actual; se manda como "Authorization: Bearer <token>".
 */
class CopApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val token: () -> String?
) {

    private fun HttpRequestBuilder.conToken() {
        token()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private inline fun <reified T> HttpRequestBuilder.json(cuerpo: T) {
        contentType(ContentType.Application.Json)   // avisamos que mandamos JSON
        setBody(cuerpo)                             // el serializador convierte el objeto a JSON
    }

    // ---------- Sesión (sin token) ----------

    suspend fun login(request: LoginRequest): HttpResponse =
        client.post("$baseUrl/login") { json(request) }

    suspend fun registro(request: RegistroRequest): HttpResponse =
        client.post("$baseUrl/registro") { json(request) }

    suspend fun recuperar(request: RecuperarRequest): HttpResponse =
        client.post("$baseUrl/recuperar") { json(request) }

    suspend fun restablecer(request: RestablecerRequest): HttpResponse =
        client.post("$baseUrl/restablecer") { json(request) }

    suspend fun configuracion(): HttpResponse =
        client.get("$baseUrl/configuracion")

    suspend fun poligonos(): HttpResponse =
        client.get("$baseUrl/poligonos")

    // ---------- Catálogo ----------

    suspend fun servicios(): HttpResponse =
        client.get("$baseUrl/servicios") { conToken() }

    /** Ejemplo: http://10.0.2.2:8080/profesionales?servicioId=FIS */
    suspend fun profesionales(servicioId: String?, todos: Boolean): HttpResponse =
        client.get("$baseUrl/profesionales") {
            conToken()
            if (servicioId != null) parameter("servicioId", servicioId)
            if (todos) parameter("todos", "true")
        }

    // ---------- Citas ----------

    /** Ejemplo de URL final: http://10.0.2.2:8080/citas?paciente=ana */
    suspend fun citas(paciente: String?, profesional: String?): HttpResponse =
        client.get("$baseUrl/citas") {
            conToken()
            if (paciente != null) parameter("paciente", paciente)
            if (profesional != null) parameter("profesional", profesional)
        }

    suspend fun fechasDisponibles(profesional: String): HttpResponse =
        client.get("$baseUrl/disponibilidad/fechas") {
            conToken()
            parameter("profesional", profesional)
        }

    suspend fun horasLibres(profesional: String, fecha: String, excluirCitaId: Int?): HttpResponse =
        client.get("$baseUrl/disponibilidad") {
            conToken()
            parameter("profesional", profesional)
            parameter("fecha", fecha)
            if (excluirCitaId != null) parameter("excluirCitaId", excluirCitaId)
        }

    suspend fun solicitarCita(request: CitaRequest): HttpResponse =
        client.post("$baseUrl/citas") {
            conToken()
            json(request)
        }

    suspend fun citaManual(request: CitaManualRequest): HttpResponse =
        client.post("$baseUrl/citas/manual") {
            conToken()
            json(request)
        }

    suspend fun cancelarCita(id: Int): HttpResponse =
        client.put("$baseUrl/citas/$id/cancelar") { conToken() }

    suspend fun reprogramarCita(id: Int, request: ReprogramarRequest): HttpResponse =
        client.put("$baseUrl/citas/$id/reprogramar") {
            conToken()
            json(request)
        }

    suspend fun cambiarEstado(id: Int, request: CambioEstadoRequest): HttpResponse =
        client.put("$baseUrl/citas/$id/estado") {
            conToken()
            json(request)
        }

    // ---------- Avisos ----------

    suspend fun notificaciones(): HttpResponse =
        client.get("$baseUrl/notificaciones") { conToken() }

    suspend fun marcarLeida(id: Int): HttpResponse =
        client.put("$baseUrl/notificaciones/$id/leida") { conToken() }

    suspend fun marcarTodasLeidas(): HttpResponse =
        client.put("$baseUrl/notificaciones/leidas") { conToken() }

    // ---------- Administración ----------

    suspend fun crearServicio(servicio: ServicioDto): HttpResponse =
        client.post("$baseUrl/servicios") {
            conToken()
            json(servicio)
        }

    suspend fun actualizarServicio(servicio: ServicioDto): HttpResponse =
        client.put("$baseUrl/servicios/${servicio.id}") {
            conToken()
            json(servicio)
        }

    suspend fun crearProfesional(request: ProfesionalRequest): HttpResponse =
        client.post("$baseUrl/profesionales") {
            conToken()
            json(request)
        }

    suspend fun actualizarProfesional(request: ProfesionalRequest): HttpResponse =
        client.put("$baseUrl/profesionales/${request.username}") {
            conToken()
            json(request)
        }

    suspend fun horarios(profesional: String): HttpResponse =
        client.get("$baseUrl/profesionales/$profesional/horarios") { conToken() }

    suspend fun crearHorario(request: HorarioRequest): HttpResponse =
        client.post("$baseUrl/horarios") {
            conToken()
            json(request)
        }

    suspend fun eliminarHorario(id: Int): HttpResponse =
        client.delete("$baseUrl/horarios/$id") { conToken() }

    suspend fun enviarReporte(request: ReporteRequest): HttpResponse =
        client.post("$baseUrl/reportes") {
            conToken()
            json(request)
        }

    suspend fun reportes(): HttpResponse =
        client.get("$baseUrl/reportes") { conToken() }

    /** COP-RI-07: enlace de descarga (se abre en el navegador, por eso el token va en la URL). */
    fun urlExportar(tipo: String, desde: String, hasta: String, servicioId: String?): String {
        val clave = (token() ?: "").encodeURLParameter()
        val servicio = if (servicioId != null) "&servicioId=$servicioId" else ""

        return "$baseUrl/exportar/agenda.$tipo?token=$clave&desde=$desde&hasta=$hasta$servicio"
    }
}
