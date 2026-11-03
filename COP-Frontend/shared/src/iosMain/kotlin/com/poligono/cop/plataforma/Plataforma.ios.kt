package com.poligono.cop.plataforma

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

/*
 * Versión para iOS (COP-RE-02: publicación futura). Lo básico funciona; las funciones
 * marcadas como "pendiente" regresan false y la app muestra un mensaje.
 */

actual fun guardarLocal(clave: String, valor: String) {
    NSUserDefaults.standardUserDefaults.setObject(valor, forKey = clave)
}

actual fun leerLocal(clave: String): String? {
    return NSUserDefaults.standardUserDefaults.stringForKey(clave)
}

actual fun borrarLocal(clave: String) {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(clave)
}

actual fun ahoraMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun desfaseZonaMinutos(): Int {
    return (NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate()) / 60).toInt()
}

actual fun nombreDispositivo(): String {
    return UIDevice.currentDevice.model + " iOS " + UIDevice.currentDevice.systemVersion
}

actual fun abrirEnlace(url: String): Boolean {
    val direccion = NSURL.URLWithString(url) ?: return false
    UIApplication.sharedApplication.openURL(direccion, options = emptyMap<Any?, Any?>(), completionHandler = null)
    return true
}

actual fun llamarTelefono(numero: String): Boolean {
    val soloNumero = numero.substringBefore("ext").filter { it.isDigit() }
    return abrirEnlace("tel:$soloNumero")
}

actual fun abrirWhatsapp(numero: String, mensaje: String): Boolean {
    val soloNumero = numero.filter { it.isDigit() }
    return abrirEnlace("https://wa.me/52$soloNumero")
}

actual fun abrirComoLlegar(latitud: Double, longitud: Double, nombre: String): Boolean {
    return abrirEnlace("https://maps.apple.com/?daddr=$latitud,$longitud")
}

// Pendiente en iOS: EventKit
actual fun agregarAlCalendario(
    titulo: String,
    descripcion: String,
    ubicacion: String,
    inicioMillis: Long,
    finMillis: Long
): Boolean {
    return false
}

// Pendiente en iOS: UNUserNotificationCenter
actual fun mostrarNotificacion(id: Int, titulo: String, mensaje: String) {
}

actual fun programarRecordatorio(id: Int, titulo: String, mensaje: String, cuandoMillis: Long) {
}

actual fun cancelarRecordatorio(id: Int) {
}

actual fun programarRevisionAvisos() {
}

actual fun cancelarRevisionAvisos() {
}

@Composable
actual fun ManejarAtras(habilitado: Boolean, alRegresar: () -> Unit) {
}

@Composable
actual fun PedirPermisoNotificaciones() {
}
