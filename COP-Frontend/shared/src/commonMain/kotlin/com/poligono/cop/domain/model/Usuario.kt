package com.poligono.cop.domain.model

/**
 * Usuario que ya inició sesión. Es el modelo que usa la app internamente
 * (no trae contraseña: la contraseña nunca sale del backend).
 */
data class Usuario(
    val username: String,
    val nombre: String,
    val rol: Rol,
    /** Solo los profesionales tienen un servicio (ej. "FIS" = Fisioterapia). */
    val servicioId: String? = null
)
