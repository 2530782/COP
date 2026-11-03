package com.poligono.cop.models

import kotlinx.serialization.Serializable

/** Así se guarda una cita en citas.json (solo ids / usernames). */
@Serializable
data class CitaRegistro(
    val id: Int,
    val servicioId: String,
    val profesional: String,   // username del profesional
    val paciente: String,      // username del paciente
    val fecha: String,         // "2026-10-12"
    val hora: String,          // "10:00"
    val estado: String         // PENDIENTE | CONFIRMADA | CANCELADA | ATENDIDA
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
    val estado: String
)
