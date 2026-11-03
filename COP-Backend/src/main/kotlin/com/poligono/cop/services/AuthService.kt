package com.poligono.cop.services

import com.poligono.cop.models.UsuarioRegistro

/** Los 4 resultados posibles de un intento de inicio de sesión. */
sealed interface ResultadoLogin {
    data class Exito(val usuario: UsuarioRegistro) : ResultadoLogin
    data class Incorrecto(val intentosRestantes: Int) : ResultadoLogin
    data class Bloqueado(val segundosRestantes: Long) : ResultadoLogin
    data object Desactivado : ResultadoLogin
}

/**
 * Lógica de autenticación.
 * COP-RNN-02: tras 3 intentos fallidos SEGUIDOS, la cuenta se bloquea temporalmente.
 *
 * Los intentos se guardan en memoria (un mapa); si reinicias el servidor se borran.
 * "reloj" se recibe como parámetro para poder simular el tiempo en las pruebas.
 */
class AuthService(
    private val usuarios: List<UsuarioRegistro>,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) {
    private class Intentos(var fallos: Int = 0, var bloqueadoHasta: Long = 0L)

    private val intentosPorUsuario = HashMap<String, Intentos>()

    // @Synchronized: si llegan dos peticiones al mismo tiempo, se atienden una por una
    @Synchronized
    fun iniciarSesion(username: String, password: String): ResultadoLogin {
        val clave = username.lowercase()
        val ahora = reloj()
        val intentos = intentosPorUsuario.getOrPut(clave) { Intentos() }

        // 1) ¿Sigue bloqueada?
        if (intentos.bloqueadoHasta > ahora) {
            val segundos = (intentos.bloqueadoHasta - ahora + 999) / 1000
            return ResultadoLogin.Bloqueado(segundos)
        }
        // 2) Si ya pasó el bloqueo, empieza de cero
        if (intentos.bloqueadoHasta != 0L) {
            intentos.fallos = 0
            intentos.bloqueadoHasta = 0L
        }

        // 3) Buscamos al usuario en lo que se leyó de usuarios.json
        val usuario = usuarios.firstOrNull {
            it.username.equals(username, ignoreCase = true) && it.password == password
        }

        if (usuario != null) {
            intentos.fallos = 0
            return if (usuario.activo) ResultadoLogin.Exito(usuario) else ResultadoLogin.Desactivado
        }

        // 4) Contraseña incorrecta: sumamos un fallo
        intentos.fallos++
        if (intentos.fallos >= MAX_INTENTOS) {
            intentos.bloqueadoHasta = ahora + BLOQUEO_MS
            return ResultadoLogin.Bloqueado(BLOQUEO_MS / 1000)
        }
        return ResultadoLogin.Incorrecto(MAX_INTENTOS - intentos.fallos)
    }

    companion object {
        const val MAX_INTENTOS = 3
        const val BLOQUEO_MS = 60_000L   // 1 minuto
    }
}
