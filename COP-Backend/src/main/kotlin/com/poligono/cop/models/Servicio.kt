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

/** Horario semanal de un profesional (día 1 = lunes ... 7 = domingo). COP-RN-14 */
@Serializable
data class HorarioDto(
    val id: Int,
    val profesional: String,
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String
)

@Serializable
data class HorarioRequest(
    val profesional: String,
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String
)

/** Un día con lugares libres (GET /disponibilidad/fechas). */
@Serializable
data class FechaDisponibleDto(
    val fecha: String,
    val espaciosLibres: Int
)
