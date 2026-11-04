package com.poligono.cop.services

import com.poligono.cop.Configuracion
import com.poligono.cop.data.Bitacora
import com.poligono.cop.data.UsuarioFila
import com.poligono.cop.data.UsuarioRepositorio
import com.poligono.cop.models.RegistroRequest
import com.poligono.cop.models.Roles
import com.poligono.cop.seguridad.Contrasenas
import io.ktor.http.HttpStatusCode

/** Los 4 resultados posibles de un intento de inicio de sesión. */
sealed interface ResultadoLogin {
    data class Exito(val usuario: UsuarioFila) : ResultadoLogin
    data class Incorrecto(val intentosRestantes: Int) : ResultadoLogin
    data class Bloqueado(val segundosRestantes: Long) : ResultadoLogin
    data object Desactivado : ResultadoLogin
}

/**
 * Lógica de autenticación.
 * COP-RNN-02: tras 3 intentos fallidos SEGUIDOS, la cuenta se bloquea temporalmente.
 *
 * Ahora los intentos se guardan en la base de datos, así que el bloqueo se respeta
 * aunque el servidor se reinicie. "reloj" se recibe como parámetro para poder
 * simular el tiempo en las pruebas.
 */
class AuthService(
    private val reloj: () -> Long = { System.currentTimeMillis() }
) {

    // @Synchronized: si llegan dos peticiones al mismo tiempo, se atienden una por una
    @Synchronized
    fun iniciarSesion(usuarioOCorreo: String, password: String): ResultadoLogin {
        val ahora = reloj()

        // Si el usuario no existe se responde igual que con contraseña incorrecta
        val usuario = UsuarioRepositorio.buscar(usuarioOCorreo)
            ?: return ResultadoLogin.Incorrecto(Configuracion.MAX_INTENTOS)

        // 1) ¿Sigue bloqueada?
        if (usuario.bloqueadoHasta > ahora) {
            val segundos = (usuario.bloqueadoHasta - ahora + 999) / 1000
            return ResultadoLogin.Bloqueado(segundos)
        }

        // 2) Si ya pasó el bloqueo, empieza de cero
        val fallosPrevios = if (usuario.bloqueadoHasta != 0L) 0 else usuario.intentosFallidos

        // 3) Contraseña correcta
        if (Contrasenas.verificar(password, usuario.contrasenaHash)) {
            UsuarioRepositorio.guardarIntentos(usuario.username, 0, 0)
            return if (usuario.activo) ResultadoLogin.Exito(usuario) else ResultadoLogin.Desactivado
        }

        // 4) Contraseña incorrecta: sumamos un fallo
        val fallos = fallosPrevios + 1

        if (fallos >= Configuracion.MAX_INTENTOS) {
            val bloqueoMs = Configuracion.segundosBloqueo * 1000
            UsuarioRepositorio.guardarIntentos(usuario.username, 0, ahora + bloqueoMs)
            Bitacora.info("Cuenta ${usuario.username} bloqueada por intentos fallidos")
            return ResultadoLogin.Bloqueado(Configuracion.segundosBloqueo)
        }

        UsuarioRepositorio.guardarIntentos(usuario.username, fallos, 0)
        return ResultadoLogin.Incorrecto(Configuracion.MAX_INTENTOS - fallos)
    }

    /** COP-RN-01 y COP-RC-05: alta de una persona usuaria con aviso de privacidad aceptado. */
    fun registrar(p: RegistroRequest): UsuarioFila {
        Validaciones.username(p.username)
        Validaciones.nombre(p.nombre)
        Validaciones.telefono(p.telefono)
        Validaciones.contrasena(p.password)

        if (p.correo.isNotBlank()) {
            Validaciones.correo(p.correo)
        }

        if (!p.aceptaPrivacidad) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "Debes aceptar el aviso de privacidad para crear tu cuenta.")
        }

        if (UsuarioRepositorio.buscar(p.username) != null) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ese nombre de usuario ya existe. Elige otro.")
        }

        if (p.correo.isNotBlank() && UsuarioRepositorio.existeCorreo(p.correo)) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ya existe una cuenta con ese correo.")
        }

        UsuarioRepositorio.crear(
            username = p.username,
            contrasenaHash = Contrasenas.cifrar(p.password),
            nombre = p.nombre,
            rol = Roles.USUARIO,
            servicioId = null,
            telefono = p.telefono.filter { it.isDigit() },
            correo = p.correo,
            aceptoPrivacidad = true
        )

        Bitacora.info("Nuevo usuario registrado: ${p.username.lowercase()}")
        return UsuarioRepositorio.buscar(p.username)!!
    }

    /** COP-RN-02: genera un código de 6 dígitos válido 15 minutos. Regresa el código o null. */
    fun generarCodigo(username: String): String? {
        val usuario = UsuarioRepositorio.buscar(username) ?: return null
        val codigo = Contrasenas.codigoRecuperacion()

        UsuarioRepositorio.guardarCodigo(usuario.username, codigo, reloj() + 15 * 60_000)
        Bitacora.info("Código de recuperación generado para ${usuario.username}")
        return codigo
    }

    fun restablecer(
        username: String,
        codigo: String,
        nuevaPassword: String
    ) {
        val usuario = UsuarioRepositorio.buscar(username)

        val valido = usuario != null &&
            usuario.codigoRecuperacion != null &&
            usuario.codigoRecuperacion == codigo.trim() &&
            usuario.codigoExpira > reloj()

        if (!valido) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "El código no es válido o ya expiró. Solicita uno nuevo.")
        }

        Validaciones.contrasena(nuevaPassword)
        UsuarioRepositorio.cambiarContrasena(usuario!!.username, Contrasenas.cifrar(nuevaPassword))
        Bitacora.info("Contraseña restablecida para ${usuario.username}")
    }
}
