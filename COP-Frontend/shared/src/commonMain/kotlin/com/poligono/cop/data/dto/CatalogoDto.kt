package com.poligono.cop.data.dto

import kotlinx.serialization.Serializable

/** Respuesta de GET /servicios (una lista de estos). */
@Serializable
data class ServicioDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val duracionMinutos: Int,
    val activo: Boolean = true
)

/** Respuesta de GET /profesionales (una lista de estos). */
@Serializable
data class ProfesionalDto(
    val username: String,
    val nombre: String,
    val servicioId: String? = null
)

/** Respuesta de GET /citas (una lista de estos). */
@Serializable
data class CitaDto(
    val id: Int,
    val servicioId: String,
    val servicio: String,
    val profesional: String,
    val profesionalNombre: String,
    val paciente: String,
    val pacienteNombre: String,
    val fecha: String,
    val hora: String,
    val estado: String
)
