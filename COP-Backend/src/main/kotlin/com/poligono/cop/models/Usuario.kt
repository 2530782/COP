package com.poligono.cop.models

import kotlinx.serialization.Serializable

/**
 * Así viene CADA usuario dentro de resources/data/usuarios.json.
 * Incluye la contraseña, por eso NUNCA se manda tal cual al frontend.
 */
@Serializable
data class UsuarioRegistro(
    val username: String,
    val password: String,
    val nombre: String,
    val rol: String,                 // ADMINISTRADOR | PERSONAL | PROFESIONAL | USUARIO
    val servicioId: String? = null,  // solo para PROFESIONAL
    val activo: Boolean = true
) {
    /** Versión pública del usuario: SIN contraseña. */
    fun aPublico(): UsuarioDto = UsuarioDto(username, nombre, rol, servicioId)
}

/** Lo que el frontend sí puede ver de un usuario. */
@Serializable
data class UsuarioDto(
    val username: String,
    val nombre: String,
    val rol: String,
    val servicioId: String? = null
)

/** Respuesta de GET /profesionales. */
@Serializable
data class ProfesionalDto(
    val username: String,
    val nombre: String,
    val servicioId: String? = null
)
