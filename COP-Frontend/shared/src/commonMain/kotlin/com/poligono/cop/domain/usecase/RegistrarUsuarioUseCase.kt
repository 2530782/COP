package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository

/** Reglas de validación de formularios (COP-RNN-09), iguales a las del backend. */
object Validar {

    private val regexCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val regexUsuario = Regex("^[a-zA-Z0-9._]{3,30}$")

    fun username(valor: String): String? =
        if (regexUsuario.matches(valor.trim())) null else "El usuario debe tener de 3 a 30 letras o números, sin espacios."

    fun nombre(valor: String): String? =
        if (valor.trim().length >= 3) null else "Escribe el nombre completo."

    fun telefono(valor: String): String? =
        if (valor.filter { it.isDigit() }.length == 10) null else "El teléfono debe tener 10 dígitos."

    fun correoOpcional(valor: String): String? =
        if (valor.isBlank() || regexCorreo.matches(valor.trim())) null else "El correo no es válido."

    fun contrasena(valor: String): String? {
        val valida = valor.length >= 6 && valor.any { it.isLetter() } && valor.any { it.isDigit() }
        return if (valida) null else "La contraseña debe tener al menos 6 caracteres, con letras y números."
    }
}

/** Caso de uso: crear cuenta (COP-RN-01) con aviso de privacidad aceptado (COP-RC-05). */
class RegistrarUsuarioUseCase(private val repository: CopRepository) {

    suspend operator fun invoke(
        username: String,
        password: String,
        confirmar: String,
        nombre: String,
        telefono: String,
        correo: String,
        aceptaPrivacidad: Boolean
    ): Result<Usuario> {
        val error = Validar.nombre(nombre)
            ?: Validar.telefono(telefono)
            ?: Validar.correoOpcional(correo)
            ?: Validar.username(username)
            ?: Validar.contrasena(password)
            ?: (if (password != confirmar) "Las contraseñas no coinciden." else null)
            ?: (if (!aceptaPrivacidad) "Debes aceptar el aviso de privacidad." else null)

        if (error != null) {
            return Result.failure(IllegalArgumentException(error))
        }

        return repository.registrar(
            username = username.trim(),
            password = password,
            nombre = nombre.trim(),
            telefono = telefono.filter { it.isDigit() },
            correo = correo.trim(),
            aceptaPrivacidad = true
        )
    }
}
