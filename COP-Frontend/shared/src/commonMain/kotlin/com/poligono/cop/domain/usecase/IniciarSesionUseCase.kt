package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository

/**
 * Caso de uso: iniciar sesión.
 * Aquí va la regla de negocio ANTES de ir al servidor (COP-RNN-09):
 * no tiene caso mandar la petición si los campos están vacíos.
 */
class IniciarSesionUseCase(private val repository: CopRepository) {

    // "operator fun invoke" permite llamar al caso de uso como si fuera una función:
    //   iniciarSesion("ana", "ana123")
    suspend operator fun invoke(username: String, password: String): Result<Usuario> {
        val usuarioLimpio = username.trim()

        if (usuarioLimpio.isEmpty() && password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe tu usuario y tu contraseña"))
        }
        if (usuarioLimpio.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe tu usuario"))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe tu contraseña"))
        }

        return repository.iniciarSesion(usuarioLimpio, password)
    }
}
