package com.poligono.cop.domain.model

/** Horario semanal de un profesional (día 1 = lunes ... 7 = domingo). COP-RN-14 */
data class Horario(
    val id: Int,
    val profesional: String,
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String
)

/** Día con lugares libres para agendar. */
data class FechaDisponible(
    val fecha: String,
    val espaciosLibres: Int
)

/** Aviso del Centro (COP-RN-11). */
data class Notificacion(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val creado: Long
)

/** Polígono (Centro de Desarrollo Comunitario) de Tulancingo. */
data class Poligono(
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

/** Reporte de falla (COP-RS-07). */
data class Reporte(
    val id: Int,
    val usuario: String,
    val descripcion: String,
    val dispositivo: String,
    val creado: Long
)

/** Valores que vienen del backend (GET /configuracion). */
data class Configuracion(
    val plazoHorasCambio: Long = 24,
    val horasRecordatorio: Int = 24,
    val minutosInactividad: Int = 10,
    val telefonoCentro: String = "775 755 8450 ext. 1135",
    val whatsappCentro: String = "7753599248"
)
