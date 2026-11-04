package com.poligono.cop.data.dto

import kotlinx.serialization.Serializable

/** Respuesta de GET /servicios (una lista de estos). También se manda al crear o editar. */
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
    val servicioId: String? = null,
    val activo: Boolean = true
)

/** POST /profesionales y PUT /profesionales/{username}. */
@Serializable
data class ProfesionalRequest(
    val username: String,
    val password: String = "",
    val nombre: String,
    val servicioId: String,
    val activo: Boolean = true
)

/** Horario semanal de un profesional (día 1 = lunes ... 7 = domingo). */
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

/** Respuesta de GET /disponibilidad/fechas. */
@Serializable
data class FechaDisponibleDto(
    val fecha: String,
    val espaciosLibres: Int
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
    val estado: String,
    val pacienteTelefono: String = "",
    val notas: String = "",
    val motivo: String = "",
    val creadoPor: String = "",
    val puedeModificar: Boolean = false
)

/** POST /citas (COP-RN-05). */
@Serializable
data class CitaRequest(
    val profesional: String,
    val fecha: String,
    val hora: String,
    val notas: String = ""
)

/** POST /citas/manual (COP-RN-15). */
@Serializable
data class CitaManualRequest(
    val profesional: String,
    val fecha: String,
    val hora: String,
    val nombreContacto: String,
    val telefonoContacto: String,
    val notas: String = ""
)

/** PUT /citas/{id}/reprogramar. */
@Serializable
data class ReprogramarRequest(
    val fecha: String,
    val hora: String,
    val motivo: String = ""
)

/** PUT /citas/{id}/estado. */
@Serializable
data class CambioEstadoRequest(
    val estado: String,
    val motivo: String = ""
)
