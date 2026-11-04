package com.poligono.cop.models

import kotlinx.serialization.Serializable

/** Estados de una cita (COP-RN-06, 12 y 18). */
object EstadosCita {
    const val PENDIENTE = "PENDIENTE"
    const val CONFIRMADA = "CONFIRMADA"
    const val RECHAZADA = "RECHAZADA"
    const val CANCELADA = "CANCELADA"
    const val ATENDIDA = "ATENDIDA"
    const val INASISTENCIA = "INASISTENCIA"

    /** Citas que todavía ocupan un horario. */
    val ACTIVOS = listOf(PENDIENTE, CONFIRMADA)
}

/** Así viene una cita en citas.json (solo ids / usernames). Se usa como dato inicial. */
@Serializable
data class CitaRegistro(
    val id: Int,
    val servicioId: String,
    val profesional: String,   // username del profesional
    val paciente: String,      // username del paciente
    val fecha: String,         // "2026-10-12"
    val hora: String,          // "10:00"
    val estado: String
)

/** Así se RESPONDE una cita: con los nombres ya resueltos para que el frontend solo la muestre. */
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
    /** true si todavía se puede cancelar o reprogramar desde la app (COP-RNN-04). */
    val puedeModificar: Boolean = false
)

/** POST /citas: el usuario solicita una cita (COP-RN-05). */
@Serializable
data class CitaRequest(
    val profesional: String,
    val fecha: String,
    val hora: String,
    val notas: String = ""
)

/** POST /citas/manual: el personal registra una cita presencial o telefónica (COP-RN-15). */
@Serializable
data class CitaManualRequest(
    val profesional: String,
    val fecha: String,
    val hora: String,
    val nombreContacto: String,
    val telefonoContacto: String,
    val notas: String = ""
)

/** PUT /citas/{id}/reprogramar (COP-RN-09 y 16). */
@Serializable
data class ReprogramarRequest(
    val fecha: String,
    val hora: String,
    val motivo: String = ""
)

/** PUT /citas/{id}/estado (COP-RN-12, 16 y 18). */
@Serializable
data class CambioEstadoRequest(
    val estado: String,
    val motivo: String = ""
)
