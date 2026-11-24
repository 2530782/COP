@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.poligono.cop.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/*
 * Implementación WEB (Kotlin/Wasm) de las funciones de Plataforma.kt.
 * Se usan las funciones de JavaScript del navegador:
 * localStorage, Date, window.open, Notification y setTimeout.
 */

// =====================================================================
// Funciones de JavaScript (solo se usan dentro de este archivo)
// =====================================================================

private fun jsGuardar(clave: String, valor: String): Unit =
    js("localStorage.setItem(clave, valor)")

private fun jsLeer(clave: String): String? =
    js("localStorage.getItem(clave)")

private fun jsBorrar(clave: String): Unit =
    js("localStorage.removeItem(clave)")

private fun jsAhora(): Double =
    js("Date.now()")

private fun jsDesfaseMinutos(): Int =
    js("-new Date().getTimezoneOffset()")

private fun jsAbrirPestana(url: String): Unit =
    js("window.open(url, '_blank')")

private fun jsIrA(url: String): Unit =
    js("window.location.href = url")

private fun jsCodificar(texto: String): String =
    js("encodeURIComponent(texto)")

private fun jsFechaIso(millis: Double): String =
    js("new Date(millis).toISOString()")

private fun jsNotificar(titulo: String, mensaje: String): Unit =
    js(
        """{
            if ('Notification' in window && Notification.permission === 'granted') {
                new Notification(titulo, { body: mensaje });
            }
        }"""
    )

private fun jsPedirPermisoNotificaciones(): Unit =
    js(
        """{
            if ('Notification' in window && Notification.permission === 'default') {
                Notification.requestPermission();
            }
        }"""
    )

private fun jsProgramar(id: Int, titulo: String, mensaje: String, retrasoMs: Double): Unit =
    js(
        """{
            window.copRecordatorios = window.copRecordatorios || {};
            if (window.copRecordatorios[id]) {
                clearTimeout(window.copRecordatorios[id]);
            }
            window.copRecordatorios[id] = setTimeout(function () {
                if ('Notification' in window && Notification.permission === 'granted') {
                    new Notification(titulo, { body: mensaje });
                }
                delete window.copRecordatorios[id];
            }, retrasoMs);
        }"""
    )

private fun jsCancelar(id: Int): Unit =
    js(
        """{
            if (window.copRecordatorios && window.copRecordatorios[id]) {
                clearTimeout(window.copRecordatorios[id]);
                delete window.copRecordatorios[id];
            }
        }"""
    )

// =====================================================================
// Almacenamiento local (sesión y citas sin conexión) -> localStorage
// =====================================================================

actual fun guardarLocal(clave: String, valor: String) {
    jsGuardar(clave, valor)
}

actual fun leerLocal(clave: String): String? {
    return jsLeer(clave)
}

actual fun borrarLocal(clave: String) {
    jsBorrar(clave)
}

// =====================================================================
// Reloj
// =====================================================================

actual fun ahoraMillis(): Long {
    return jsAhora().toLong()
}

actual fun desfaseZonaMinutos(): Int {
    return jsDesfaseMinutos()
}

actual fun nombreDispositivo(): String {
    return "Navegador web"
}

// =====================================================================
// Acciones del sistema
// =====================================================================

actual fun abrirEnlace(url: String): Boolean {
    jsAbrirPestana(url)
    return true
}

actual fun llamarTelefono(numero: String): Boolean {
    // En computadora abre la app de llamadas que tenga configurada (si hay)
    jsIrA("tel:$numero")
    return true
}

actual fun abrirWhatsapp(numero: String, mensaje: String): Boolean {
    val soloDigitos = numero.filter { it.isDigit() }
    val url = "https://wa.me/$soloDigitos?text=${jsCodificar(mensaje)}"

    jsAbrirPestana(url)
    return true
}

actual fun abrirComoLlegar(latitud: Double, longitud: Double, nombre: String): Boolean {
    val url = "https://www.google.com/maps/dir/?api=1&destination=$latitud,$longitud"

    jsAbrirPestana(url)
    return true
}

actual fun agregarAlCalendario(
    titulo: String,
    descripcion: String,
    ubicacion: String,
    inicioMillis: Long,
    finMillis: Long
): Boolean {
    // En web se abre Google Calendar con el evento ya lleno
    val inicio = formatoCalendario(inicioMillis)
    val fin = formatoCalendario(finMillis)

    val url = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
            "&text=${jsCodificar(titulo)}" +
            "&details=${jsCodificar(descripcion)}" +
            "&location=${jsCodificar(ubicacion)}" +
            "&dates=$inicio/$fin"

    jsAbrirPestana(url)
    return true
}

/** Convierte milisegundos al formato de Google Calendar: 20261124T223000Z */
private fun formatoCalendario(millis: Long): String {
    val iso = jsFechaIso(millis.toDouble())   // 2026-11-24T22:30:00.000Z

    return iso
        .substringBefore(".")
        .replace("-", "")
        .replace(":", "") + "Z"
}

// =====================================================================
// Notificaciones locales y recordatorios
// =====================================================================

actual fun mostrarNotificacion(id: Int, titulo: String, mensaje: String) {
    jsNotificar(titulo, mensaje)
}

actual fun programarRecordatorio(id: Int, titulo: String, mensaje: String, cuandoMillis: Long) {
    val retraso = cuandoMillis - ahoraMillis()

    // En web el recordatorio solo funciona mientras la página esté abierta.
    // setTimeout no acepta más de ~24 días.
    if (retraso > 0 && retraso < Int.MAX_VALUE) {
        jsProgramar(id, titulo, mensaje, retraso.toDouble())
    }
}

actual fun cancelarRecordatorio(id: Int) {
    jsCancelar(id)
}

actual fun programarRevisionAvisos() {
    // El navegador no permite tareas en segundo plano como WorkManager.
    // En web los avisos se revisan al abrir la página.
}

actual fun cancelarRevisionAvisos() {
    // Nada que cancelar en web
}

// =====================================================================
// Composables de plataforma
// =====================================================================

@Composable
actual fun ManejarAtras(habilitado: Boolean, alRegresar: () -> Unit) {
    // En web se usa el botón "atrás" del propio navegador
}

@Composable
actual fun PedirPermisoNotificaciones() {
    LaunchedEffect(Unit) {
        jsPedirPermisoNotificaciones()
    }
}