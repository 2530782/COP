package com.poligono.cop.domain.model

/** Estados posibles de una cita (COP-RN-06, 12 y 18). */
enum class EstadoCita(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    RECHAZADA("No aceptada"),
    CANCELADA("Cancelada"),
    ATENDIDA("Atendida"),
    INASISTENCIA("No asistió");

    /** true si la cita todavía ocupa un horario. */
    val activa: Boolean
        get() = this == PENDIENTE || this == CONFIRMADA

    companion object {
        fun desdeTexto(texto: String): EstadoCita =
            entries.firstOrNull { it.name.equals(texto, ignoreCase = true) } ?: PENDIENTE
    }
}

/** Cita ya lista para mostrarse en pantalla (con nombres, no solo ids). */
data class Cita(
    val id: Int,
    val servicio: String,
    val profesional: String,          // nombre del profesional
    val paciente: String,             // nombre del paciente
    val fecha: String,                // formato "2026-10-12"
    val hora: String,                 // formato "10:00"
    val estado: EstadoCita,
    val servicioId: String = "",
    val profesionalUsername: String = "",
    val pacienteUsername: String = "",
    val pacienteTelefono: String = "",
    val notas: String = "",
    val motivo: String = "",
    val creadoPor: String = "",
    /** El backend dice si todavía se puede cancelar o reprogramar (COP-RNN-04). */
    val puedeModificar: Boolean = false
)
