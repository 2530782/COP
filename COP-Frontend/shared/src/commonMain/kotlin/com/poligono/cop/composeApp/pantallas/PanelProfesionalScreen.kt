package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.TarjetaCita
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol PROFESIONAL (fisioterapeuta, nutrióloga, psicólogo).
 * Solo ve SU agenda, con el nombre de cada paciente.
 */
@Composable
fun PanelProfesionalScreen(usuario: Usuario, onCerrarSesion: () -> Unit) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                estado.citas.count { it.estado == EstadoCita.PENDIENTE } to "Por confirmar",
                estado.citas.count { it.estado == EstadoCita.CONFIRMADA } to "Confirmadas",
                estado.citas.count { it.estado == EstadoCita.ATENDIDA } to "Atendidas"
            )
        }

        item { TituloSeccion("Mi agenda") }
        if (estado.citas.isEmpty()) {
            item { MensajeVacio("No tienes citas asignadas.") }
        } else {
            items(estado.citas, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = false)
            }
        }
    }
}
