package com.poligono.cop.plataforma

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Contexto de la aplicación. MainActivity llama a ContextoAndroid.iniciar(applicationContext)
 * y los Workers también, porque pueden ejecutarse con la app cerrada.
 */
object ContextoAndroid {

    lateinit var app: Context
        private set

    const val CANAL_CITAS = "cop_citas"

    fun iniciar(contexto: Context) {
        app = contexto.applicationContext
        crearCanal()
    }

    fun preferencias(): SharedPreferences {
        return app.getSharedPreferences("cop_datos", Context.MODE_PRIVATE)
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_CITAS,
                "Citas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            canal.description = "Recordatorios y cambios en tus citas"

            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }
}

// ---------- Almacenamiento local ----------

actual fun guardarLocal(clave: String, valor: String) {
    ContextoAndroid.preferencias()
        .edit()
        .putString(clave, valor)
        .apply()
}

actual fun leerLocal(clave: String): String? {
    return ContextoAndroid.preferencias().getString(clave, null)
}

actual fun borrarLocal(clave: String) {
    ContextoAndroid.preferencias()
        .edit()
        .remove(clave)
        .apply()
}

// ---------- Reloj ----------

actual fun ahoraMillis(): Long {
    return System.currentTimeMillis()
}

actual fun desfaseZonaMinutos(): Int {
    return TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
}

actual fun nombreDispositivo(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
}

// ---------- Acciones del sistema ----------

private fun iniciarActividad(intent: Intent): Boolean {
    return try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextoAndroid.app.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun abrirEnlace(url: String): Boolean {
    return iniciarActividad(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

actual fun llamarTelefono(numero: String): Boolean {
    // ACTION_DIAL abre el marcador sin necesitar permiso de llamadas
    val soloNumero = numero.substringBefore("ext").filter { it.isDigit() }
    return iniciarActividad(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$soloNumero")))
}

actual fun abrirWhatsapp(numero: String, mensaje: String): Boolean {
    val soloNumero = numero.filter { it.isDigit() }
    val url = "https://wa.me/52$soloNumero?text=${Uri.encode(mensaje)}"
    return iniciarActividad(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

actual fun abrirComoLlegar(latitud: Double, longitud: Double, nombre: String): Boolean {
    // Google Maps u otra app de mapas instalada; si no hay, se abre en el navegador
    val geo = Uri.parse("geo:0,0?q=$latitud,$longitud(${Uri.encode(nombre)})")

    if (iniciarActividad(Intent(Intent.ACTION_VIEW, geo))) {
        return true
    }

    val web = "https://www.google.com/maps/dir/?api=1&destination=$latitud,$longitud"
    return abrirEnlace(web)
}

actual fun agregarAlCalendario(
    titulo: String,
    descripcion: String,
    ubicacion: String,
    inicioMillis: Long,
    finMillis: Long
): Boolean {
    val intent = Intent(Intent.ACTION_INSERT)
    intent.data = CalendarContract.Events.CONTENT_URI
    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicioMillis)
    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, finMillis)
    intent.putExtra(CalendarContract.Events.TITLE, titulo)
    intent.putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
    intent.putExtra(CalendarContract.Events.EVENT_LOCATION, ubicacion)

    return iniciarActividad(intent)
}

// ---------- Notificaciones y recordatorios ----------

@SuppressLint("MissingPermission")
actual fun mostrarNotificacion(id: Int, titulo: String, mensaje: String) {
    val contexto = ContextoAndroid.app

    val conPermiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    // COP-RNN-07: si no hay permiso, el aviso se ve dentro de la app (pantalla de inicio)
    if (!conPermiso) {
        return
    }

    val abrirApp = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: Intent()
    val pendiente = PendingIntent.getActivity(
        contexto,
        id,
        abrirApp,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificacion = NotificationCompat.Builder(contexto, ContextoAndroid.CANAL_CITAS)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setContentIntent(pendiente)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(contexto).notify(id, notificacion)
}

actual fun programarRecordatorio(id: Int, titulo: String, mensaje: String, cuandoMillis: Long) {
    val retraso = cuandoMillis - System.currentTimeMillis()

    if (retraso <= 0) {
        return
    }

    val datos = workDataOf(
        "id" to id,
        "titulo" to titulo,
        "mensaje" to mensaje
    )

    val peticion = OneTimeWorkRequestBuilder<RecordatorioWorker>()
        .setInitialDelay(retraso, TimeUnit.MILLISECONDS)
        .setInputData(datos)
        .build()

    WorkManager.getInstance(ContextoAndroid.app).enqueueUniqueWork(
        "recordatorio_$id",
        ExistingWorkPolicy.REPLACE,
        peticion
    )
}

actual fun cancelarRecordatorio(id: Int) {
    WorkManager.getInstance(ContextoAndroid.app).cancelUniqueWork("recordatorio_$id")
}

actual fun programarRevisionAvisos() {
    val restricciones = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val peticion = PeriodicWorkRequestBuilder<RevisarAvisosWorker>(15, TimeUnit.MINUTES)
        .setConstraints(restricciones)
        .build()

    WorkManager.getInstance(ContextoAndroid.app).enqueueUniquePeriodicWork(
        "revisar_avisos",
        ExistingPeriodicWorkPolicy.KEEP,
        peticion
    )
}

actual fun cancelarRevisionAvisos() {
    val manager = WorkManager.getInstance(ContextoAndroid.app)
    manager.cancelUniqueWork("revisar_avisos")
    manager.cancelAllWork()
}

// ---------- Composables ----------

@Composable
actual fun ManejarAtras(habilitado: Boolean, alRegresar: () -> Unit) {
    BackHandler(enabled = habilitado, onBack = alRegresar)
}

@Composable
actual fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!concedido) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
