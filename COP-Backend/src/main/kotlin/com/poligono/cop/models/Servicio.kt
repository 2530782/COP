package com.poligono.cop.models

import kotlinx.serialization.Serializable

/** Servicio del Centro, tal como está en servicios.json y como se responde en GET /servicios. */
@Serializable
data class Servicio(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val duracionMinutos: Int,
    val activo: Boolean = true
)
