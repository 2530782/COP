package com.poligono.cop.services

import io.ktor.http.HttpStatusCode

/** COP-RNN-09: validación de datos con mensajes claros. */
object Validaciones {

    private val regexCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val regexHora = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
    private val regexUsuario = Regex("^[a-zA-Z0-9._]{3,30}$")

    fun username(valor: String) {
        if (!regexUsuario.matches(valor.trim())) {
            falla("El usuario debe tener de 3 a 30 letras o números, sin espacios.")
        }
    }

    fun nombre(valor: String) {
        if (valor.trim().length < 3) {
            falla("Escribe el nombre completo (mínimo 3 letras).")
        }

        if (valor.length > 120) {
            falla("El nombre es demasiado largo.")
        }
    }

    fun telefono(valor: String) {
        if (valor.filter { it.isDigit() }.length != 10) {
            falla("El teléfono debe tener 10 dígitos.")
        }
    }

    fun correo(valor: String) {
        if (!regexCorreo.matches(valor.trim())) {
            falla("El correo electrónico no es válido.")
        }
    }

    fun contrasena(valor: String) {
        val tieneLetra = valor.any { it.isLetter() }
        val tieneNumero = valor.any { it.isDigit() }

        if (valor.length < 6 || !tieneLetra || !tieneNumero) {
            falla("La contraseña debe tener al menos 6 caracteres, con letras y números.")
        }
    }

    fun texto(
        valor: String,
        campo: String,
        maximo: Int
    ) {
        if (valor.isBlank()) {
            falla("El campo $campo es obligatorio.")
        }

        if (valor.length > maximo) {
            falla("El campo $campo no debe pasar de $maximo caracteres.")
        }
    }

    fun horario(
        diaSemana: Int,
        horaInicio: String,
        horaFin: String
    ) {
        if (diaSemana !in 1..7) {
            falla("El día debe ser de 1 (lunes) a 7 (domingo).")
        }

        if (!regexHora.matches(horaInicio) || !regexHora.matches(horaFin)) {
            falla("Las horas deben tener el formato HH:MM, por ejemplo 09:00.")
        }

        if (horaInicio >= horaFin) {
            falla("La hora de inicio debe ser antes de la hora de fin.")
        }
    }

    private fun falla(mensaje: String): Nothing {
        throw ErrorNegocio(HttpStatusCode.BadRequest, mensaje)
    }
}
