package com.poligono.cop.domain.model

/** Profesional que atiende un servicio (COP-RN-04). */
data class Profesional(
    val username: String,
    val nombre: String,
    val servicioId: String?,
    val activo: Boolean = true
)
