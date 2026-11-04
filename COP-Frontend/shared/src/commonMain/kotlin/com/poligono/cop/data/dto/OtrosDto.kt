package com.poligono.cop.data.dto

import kotlinx.serialization.Serializable

/** GET /notificaciones (COP-RN-11). */
@Serializable
data class NotificacionDto(
    val id: Int,
    val citaId: Int? = null,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val creado: Long
)

/** GET /poligonos: polígonos de Tulancingo para el mapa con GPS. */
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
    val ubicacionAproximada: Boolean = false
)

/** POST /reportes (COP-RS-07). */
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

/** GET /configuracion. */
@Serializable
data class ConfiguracionDto(
    val plazoHorasCambio: Long = 24,
    val horasRecordatorio: Int = 24,
    val minutosInactividad: Int = 10,
    val telefonoCentro: String = "775 755 8450 ext. 1135",
    val whatsappCentro: String = "7753599248",
    val fechaHoy: String = ""
)
