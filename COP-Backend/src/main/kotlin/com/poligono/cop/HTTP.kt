package com.poligono.cop

import com.poligono.cop.data.Bitacora
import com.poligono.cop.models.RespuestaError
import com.poligono.cop.services.ErrorNegocio
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.event.Level

/** Configuración HTTP: CORS y manejo de errores. */
fun Application.configureHTTP() {

    // CORS: permite que una app WEB (otro origen) llame a este servidor (COP-RE-03, BTP futura)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    // StatusPages: si algo truena, respondemos un JSON claro en lugar de una página de error
    install(StatusPages) {
        // Reglas de negocio: mensaje claro con su código (400, 401, 403, 404, 409...)
        exception<ErrorNegocio> { call, causa ->
            call.respond(
                causa.estado,
                RespuestaError(exito = false, mensaje = causa.message ?: "Ocurrió un error.")
            )
        }
        // JSON mal formado o campos faltantes -> 400
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                RespuestaError(exito = false, mensaje = "La petición no tiene el formato esperado")
            )
        }
        // Cualquier otro error -> 500 y se registra en la bitácora
        exception<Throwable> { call, causa ->
            Bitacora.error("Error no controlado", causa)
            call.respond(
                HttpStatusCode.InternalServerError,
                RespuestaError(exito = false, mensaje = "Error interno del servidor")
            )
        }
    }
}

/** Bitácora: imprime en consola cada petición que llega (COP-RS-06). */
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }
}
