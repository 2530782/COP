package com.poligono.cop.models

import kotlinx.serialization.Serializable

/** Aviso del Centro para el usuario (COP-RN-11). */
@Serializable
data class NotificacionDto(
    val id: Int,
    val citaId: Int? = null,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val creado: Long
)

/** Polígono (Centro de Desarrollo Comunitario) para el mapa con GPS. */
@Serializable
data class PoligonoDto(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val horario: String,
    val telefono: String,
    val servicios: String,
    val esPrincipal: Boolean,
    val ubicacionAproximada: Boolean
)

@Serializable
data class PoligonoRequest(
    val nombre: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val horario: String,
    val telefono: String,
    val servicios: String,
    val esPrincipal: Boolean = false,
    val ubicacionAproximada: Boolean = false
)

/** Reporte de falla enviado desde la app (COP-RS-07). */
@Serializable
data class ReporteRequest(
    val descripcion: String,
    val dispositivo: String = ""
)

@Serializable
data class ReporteDto(
    val id: Int,
    val usuario: String,
    val descripcion: String,
    val dispositivo: String,
    val creado: Long
)

/** GET /configuracion: valores que la app necesita conocer. */
@Serializable
data class ConfiguracionDto(
    val plazoHorasCambio: Long,
    val horasRecordatorio: Int,
    val minutosInactividad: Int,
    val telefonoCentro: String,
    val whatsappCentro: String,
    val fechaHoy: String
)
