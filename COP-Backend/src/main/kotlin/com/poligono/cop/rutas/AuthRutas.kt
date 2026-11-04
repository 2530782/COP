package com.poligono.cop.rutas

import com.poligono.cop.Configuracion
import com.poligono.cop.models.LoginRequest
import com.poligono.cop.models.LoginResponse
import com.poligono.cop.models.RecuperarRequest
import com.poligono.cop.models.RecuperarResponse
import com.poligono.cop.models.RegistroRequest
import com.poligono.cop.models.RespuestaOk
import com.poligono.cop.models.RestablecerRequest
import com.poligono.cop.seguridad.Tokens
import com.poligono.cop.services.AuthService
import com.poligono.cop.services.ResultadoLogin
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** COP-RN-01 registro, COP-RN-02 sesión y recuperación, COP-RNN-02 bloqueo. */
fun Route.rutasAuth(auth: AuthService) {

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
                    usuario = resultado.usuario.aPublico(),
                    token = Tokens.generar(resultado.usuario.username, resultado.usuario.rol)
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

    // COP-RN-01: el registro deja la sesión iniciada
    post("/registro") {
        val peticion = call.receive<RegistroRequest>()
        val usuario = auth.registrar(peticion)

        call.respond(
            HttpStatusCode.Created,
            LoginResponse(
                exito = true,
                mensaje = "Cuenta creada. Bienvenido, ${usuario.nombre}",
                usuario = usuario.aPublico(),
                token = Tokens.generar(usuario.username, usuario.rol)
            )
        )
    }

    post("/recuperar") {
        val peticion = call.receive<RecuperarRequest>()
        val codigo = auth.generarCodigo(peticion.username)

        // No se revela si el usuario existe o no
        call.respond(
            RecuperarResponse(
                exito = true,
                mensaje = "Si la cuenta existe, se generó un código de 6 dígitos válido por 15 minutos.",
                // Mientras no haya servicio de correo, en modo demo el código se regresa aquí
                codigoDemo = if (Configuracion.modoDemo) codigo else null
            )
        )
    }

    post("/restablecer") {
        val peticion = call.receive<RestablecerRequest>()
        auth.restablecer(peticion.username, peticion.codigo, peticion.nuevaPassword)

        call.respond(RespuestaOk(mensaje = "Tu contraseña se cambió. Ya puedes iniciar sesión."))
    }
}
