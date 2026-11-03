package com.poligono.cop.plataforma

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.poligono.cop.data.local.Avisos
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Rol

/** COP-RN-10: muestra el recordatorio 24 horas antes de la cita. */
class RecordatorioWorker(
    contexto: Context,
    parametros: WorkerParameters
) : Worker(contexto, parametros) {

    override fun doWork(): Result {
        ContextoAndroid.iniciar(applicationContext)

        val id = inputData.getInt("id", 0)
        val titulo = inputData.getString("titulo") ?: "Recordatorio de tu cita"
        val mensaje = inputData.getString("mensaje") ?: "Tienes una cita en el Polígono Guadalupe."

        mostrarNotificacion(id, titulo, mensaje)
        return Result.success()
    }
}

/**
 * COP-RN-11: cada 15 minutos revisa si el Centro confirmó, modificó o canceló una cita
 * y lo muestra como notificación, aunque la app esté cerrada.
 */
class RevisarAvisosWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        ContextoAndroid.iniciar(applicationContext)

        val repository = Dependencias.repository
        val usuario = repository.sesionGuardada()

        if (usuario == null || usuario.rol != Rol.USUARIO) {
            return Result.success()
        }

        repository.obtenerNotificaciones().onSuccess { Avisos.mostrarNuevos(it) }
        repository.obtenerCitas(paciente = usuario.username).onSuccess { Avisos.sincronizarRecordatorios(it) }

        // Sin internet no pasa nada: se vuelve a intentar en la siguiente revisión
        return Result.success()
    }
}
