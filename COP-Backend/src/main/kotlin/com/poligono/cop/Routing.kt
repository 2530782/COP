package com.poligono.cop

import com.poligono.cop.data.JsonDataSource
import com.poligono.cop.models.LoginRequest
import com.poligono.cop.models.LoginResponse
import com.poligono.cop.models.ProfesionalDto
import com.poligono.cop.services.AuthService
import com.poligono.cop.services.ResultadoLogin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

/**
 * RUTAS (endpoints) del backend COP.
 *
 *   GET  /               -> prueba rápida de que el servidor está vivo
 *   POST /login          -> inicia sesión con usuario y contraseña (usuarios.json)
 *   GET  /servicios      -> catálogo de servicios (servicios.json)
 *   GET  /profesionales  -> profesionales (usuarios.json con rol PROFESIONAL)
 *   GET  /citas          -> citas (citas.json). Filtros opcionales:
 *                           /citas?paciente=ana   /citas?profesional=fisio
 */
fun Application.configureRouting(datos: JsonDataSource, auth: AuthService) {
    routing {

        get("/") {
            call.respondText("Servidor COP funcionando")
        }

        post("/login") {
            // El serializador convierte el JSON del cuerpo en un LoginRequest
            val peticion = call.receive<LoginRequest>()

            // COP-RNN-09: validar datos obligatorios
            if (peticion.username.isBlank() || peticion.password.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    LoginResponse(exito = false, mensaje = "Usuario y contraseña son obligatorios")
                )
                return@post
            }

            when (val resultado = auth.iniciarSesion(peticion.username.trim(), peticion.password)) {
                is ResultadoLogin.Exito -> call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        exito = true,
                        mensaje = "Bienvenido, ${resultado.usuario.nombre}",
                        usuario = resultado.usuario.aPublico()
                    )
                )

                is ResultadoLogin.Incorrecto -> call.respond(
                    HttpStatusCode.Unauthorized,   // 401
                    LoginResponse(
                        exito = false,
                        mensaje = "Usuario o contraseña incorrectos. " +
                            "Te quedan ${resultado.intentosRestantes} intento(s)."
                    )
                )

                is ResultadoLogin.Bloqueado -> call.respond(
                    HttpStatusCode.Locked,         // 423 (COP-RNN-02)
                    LoginResponse(
                        exito = false,
                        mensaje = "Cuenta bloqueada por 3 intentos fallidos. " +
                            "Intenta de nuevo en ${resultado.segundosRestantes} segundos."
                    )
                )

                ResultadoLogin.Desactivado -> call.respond(
                    HttpStatusCode.Forbidden,      // 403
                    LoginResponse(
                        exito = false,
                        mensaje = "Tu cuenta está desactivada. Acude al Centro para más información."
                    )
                )
            }
        }

        get("/servicios") {
            call.respond(datos.servicios)
        }

        get("/profesionales") {
            val profesionales = datos.usuarios
                .filter { it.rol == "PROFESIONAL" && it.activo }
                .map { ProfesionalDto(it.username, it.nombre, it.servicioId) }
            call.respond(profesionales)
        }

        get("/citas") {
            // Parámetros opcionales de la URL (?paciente=... / ?profesional=...)
            val paciente = call.request.queryParameters["paciente"]
            val profesional = call.request.queryParameters["profesional"]
            call.respond(datos.citasDetalladas(paciente, profesional))
        }
    }
}
