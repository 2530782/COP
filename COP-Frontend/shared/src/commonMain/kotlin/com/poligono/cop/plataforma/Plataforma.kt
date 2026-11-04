package com.poligono.cop.plataforma

import androidx.compose.runtime.Composable

/*
 * Funciones que dependen del sistema operativo (expect).
 * La implementación de Android está en androidMain/plataforma/Plataforma.android.kt
 * y la de iOS en iosMain/plataforma/Plataforma.ios.kt.
 */

// ---------- Almacenamiento local (sesión y citas sin conexión) ----------

expect fun guardarLocal(clave: String, valor: String)

expect fun leerLocal(clave: String): String?

expect fun borrarLocal(clave: String)

// ---------- Reloj ----------

/** Milisegundos desde 1970 (UTC). */
expect fun ahoraMillis(): Long

/** Diferencia de la zona del teléfono con UTC, en minutos (Tulancingo: -360). */
expect fun desfaseZonaMinutos(): Int

expect fun nombreDispositivo(): String

// ---------- Acciones del sistema (devuelven false si no se pudo) ----------

expect fun abrirEnlace(url: String): Boolean

expect fun llamarTelefono(numero: String): Boolean

expect fun abrirWhatsapp(numero: String, mensaje: String): Boolean

/** Abre la app de mapas con la ruta hasta el punto. */
expect fun abrirComoLlegar(latitud: Double, longitud: Double, nombre: String): Boolean

/** COP-RI-04: agrega la cita al calendario del teléfono. */
expect fun agregarAlCalendario(
    titulo: String,
    descripcion: String,
    ubicacion: String,
    inicioMillis: Long,
    finMillis: Long
): Boolean

// ---------- Notificaciones locales y recordatorios ----------

expect fun mostrarNotificacion(id: Int, titulo: String, mensaje: String)

/** COP-RN-10: recordatorio programado para [cuandoMillis]. */
expect fun programarRecordatorio(id: Int, titulo: String, mensaje: String, cuandoMillis: Long)

expect fun cancelarRecordatorio(id: Int)

/** Revisa avisos nuevos en segundo plano (cada 15 minutos, mínimo del sistema). */
expect fun programarRevisionAvisos()

expect fun cancelarRevisionAvisos()

// ---------- Composables de plataforma ----------

/** Botón "atrás" del sistema. */
@Composable
expect fun ManejarAtras(habilitado: Boolean, alRegresar: () -> Unit)

/** Pide permiso para mostrar notificaciones (Android 13+). */
@Composable
expect fun PedirPermisoNotificaciones()
