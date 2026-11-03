package com.poligono.cop.data.local

import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Notificacion
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.plataforma.ahoraMillis
import com.poligono.cop.plataforma.cancelarRecordatorio
import com.poligono.cop.plataforma.mostrarNotificacion
import com.poligono.cop.plataforma.programarRecordatorio

/** COP-RN-10, COP-RN-11, COP-RI-02 y COP-RNN-07: recordatorios y avisos de citas. */
object Avisos {

    private const val HORA_MS = 3_600_000L

    /** Programa un recordatorio 24 h antes de cada cita activa y quita los de citas que ya no aplican. */
    fun sincronizarRecordatorios(citas: List<Cita>) {
        val ahora = ahoraMillis()
        val anticipacion = AlmacenLocal.leerConfiguracion().horasRecordatorio * HORA_MS

        for (cita in citas) {
            val momentoAviso = FechasApp.millisDe(cita.fecha, cita.hora) - anticipacion

            if (cita.estado.activa && momentoAviso > ahora) {
                programarRecordatorio(
                    id = cita.id,
                    titulo = "Recordatorio de tu cita",
                    mensaje = "Mañana tienes ${cita.servicio} a las ${cita.hora} en el Polígono Guadalupe.",
                    cuandoMillis = momentoAviso
                )
            } else {
                cancelarRecordatorio(cita.id)
            }
        }
    }

    /** COP-RNN-07: citas de las próximas 24 horas, para mostrarlas DENTRO de la app. */
    fun citasProximas(citas: List<Cita>): List<Cita> {
        val ahora = ahoraMillis()
        val limite = ahora + AlmacenLocal.leerConfiguracion().horasRecordatorio * HORA_MS

        return citas.filter { cita ->
            cita.estado.activa && FechasApp.millisDe(cita.fecha, cita.hora) in ahora..limite
        }
    }

    /** Muestra como notificación del teléfono los avisos que todavía no se habían mostrado. */
    fun mostrarNuevos(avisos: List<Notificacion>) {
        val mostrados = AlmacenLocal.avisosMostrados().toMutableSet()

        // Recién iniciada la sesión: no se muestran de golpe los avisos viejos
        if (mostrados.isEmpty()) {
            AlmacenLocal.guardarAvisosMostrados(avisos.map { it.id }.toSet() + setOf(0))
            return
        }

        val nuevos = avisos.filter { !it.leida && it.id !in mostrados }

        for (aviso in nuevos) {
            // Se suma 100000 para que no choque con el id de los recordatorios
            mostrarNotificacion(100_000 + aviso.id, aviso.titulo, aviso.mensaje)
            mostrados.add(aviso.id)
        }

        if (nuevos.isNotEmpty()) {
            AlmacenLocal.guardarAvisosMostrados(mostrados)
        }
    }
}
