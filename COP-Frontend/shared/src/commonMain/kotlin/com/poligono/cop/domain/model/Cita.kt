package com.poligono.cop.domain.model

/** Estados posibles de una cita (COP-RN-06). */
enum class EstadoCita(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada"),
    ATENDIDA("Atendida");

    companion object {
        fun desdeTexto(texto: String): EstadoCita =
            entries.firstOrNull { it.name.equals(texto, ignoreCase = true) } ?: PENDIENTE
    }
}

/** Cita ya lista para mostrarse en pantalla (con nombres, no solo ids). */
data class Cita(
    val id: Int,
    val servicio: String,
    val profesional: String,
    val paciente: String,
    val fecha: String,   // formato "2026-10-12"
    val hora: String,    // formato "10:00"
    val estado: EstadoCita
)
