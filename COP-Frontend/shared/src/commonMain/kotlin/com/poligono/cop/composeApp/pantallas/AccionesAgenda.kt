package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.poligono.cop.composeApp.componentes.DialogoConfirmar
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.util.FechasApp

/** Una acción pendiente de confirmar en el diálogo. */
data class AccionCita(
    val cita: Cita,
    val estado: EstadoCita,
    val titulo: String,
    val pedirMotivo: Boolean
)

/**
 * Botones de cada cita en la agenda:
 *  - Personal: confirmar o rechazar (COP-RN-12), modificar o cancelar (COP-RN-16)
 *  - Personal y profesional: atendida o no asistió (COP-RN-18), cuando ya llegó el día
 */
@Composable
fun BotonesAgenda(
    cita: Cita,
    esPersonal: Boolean,
    onAccion: (AccionCita) -> Unit,
    onModificar: () -> Unit
) {
    val yaLlego = cita.fecha <= FechasApp.hoy()

    if (esPersonal && cita.estado == EstadoCita.PENDIENTE) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAccion(AccionCita(cita, EstadoCita.CONFIRMADA, "¿Confirmar la cita?", false)) }) {
                Text("Confirmar")
            }
            OutlinedButton(onClick = { onAccion(AccionCita(cita, EstadoCita.RECHAZADA, "¿Rechazar la solicitud?", true)) }) {
                Text("Rechazar")
            }
        }
    }

    if (esPersonal && cita.estado.activa) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onModificar) {
                Text("Modificar")
            }
            OutlinedButton(onClick = { onAccion(AccionCita(cita, EstadoCita.CANCELADA, "¿Cancelar la cita?", true)) }) {
                Text("Cancelar")
            }
        }
    }

    if (cita.estado.activa && yaLlego) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAccion(AccionCita(cita, EstadoCita.ATENDIDA, "¿Marcar como atendida?", false)) }) {
                Text("Atendida")
            }
            OutlinedButton(onClick = { onAccion(AccionCita(cita, EstadoCita.INASISTENCIA, "¿Marcar que no asistió?", false)) }) {
                Text("No asistió")
            }
        }
    }
}

/** Diálogo de confirmación de una AccionCita. */
@Composable
fun DialogoAccion(
    accion: AccionCita,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    DialogoConfirmar(
        titulo = accion.titulo,
        texto = "${accion.cita.paciente} · ${accion.cita.servicio} · ${FechasApp.larga(accion.cita.fecha)} ${accion.cita.hora}",
        textoConfirmar = "Aceptar",
        pedirMotivo = accion.pedirMotivo,
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )
}
