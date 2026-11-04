package com.poligono.cop

import com.poligono.cop.data.UsuarioRepositorio
import com.poligono.cop.models.RespuestaError
import com.poligono.cop.models.UsuarioDto
import com.poligono.cop.seguridad.Tokens
import com.poligono.cop.services.ErrorNegocio
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

/** Nombre de la autenticación que usan las rutas protegidas: authenticate(SESION) { ... } */
const val SESION = "sesion"

/**
 * Autenticación con token JWT (COP-RC-04 confidencialidad).
 * El login regresa un token; la app lo manda en "Authorization: Bearer <token>".
 */
fun Application.configureSeguridad() {
    install(Authentication) {
        jwt(SESION) {
            realm = Tokens.REALM
            verifier(Tokens.verificador)

            validate { credencial ->
                if (credencial.payload.getClaim("username").asString() != null) {
                    JWTPrincipal(credencial.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    RespuestaError(exito = false, mensaje = "Tu sesión expiró. Inicia sesión de nuevo.")
                )
            }
        }
    }
}

/** Usuario dueño del token de la petición (o null si no mandó token). */
fun ApplicationCall.usuarioOpcional(): UsuarioDto? {
    val principal = principal<JWTPrincipal>() ?: return null
    val username = principal.payload.getClaim("username").asString() ?: return null
    val usuario = UsuarioRepositorio.buscar(username) ?: return null

    return if (usuario.activo) usuario.aPublico() else null
}

/** Usuario dueño del token. Si no hay sesión válida responde 401. */
fun ApplicationCall.usuarioActual(): UsuarioDto {
    return usuarioOpcional()
        ?: throw ErrorNegocio(HttpStatusCode.Unauthorized, "Inicia sesión para continuar.")
}

/** COP-RI-06: cada rol solo entra a lo que le corresponde. */
fun UsuarioDto.requerirRol(vararg roles: String) {
    if (rol !in roles) {
        throw ErrorNegocio(HttpStatusCode.Forbidden, "No tienes permiso para esta acción.")
    }
}

fun ApplicationCall.parametro(nombre: String): String {
    return parameters[nombre]
        ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "Falta $nombre en la ruta.")
}

fun ApplicationCall.idDeRuta(): Int {
    return parametro("id").toIntOrNull()
        ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "El id debe ser un número.")
}

/** Parámetro opcional de la URL (?nombre=valor). Vacío = null. */
fun ApplicationCall.consulta(nombre: String): String? {
    val valor = request.queryParameters[nombre]
    return if (valor.isNullOrBlank()) null else valor
}
