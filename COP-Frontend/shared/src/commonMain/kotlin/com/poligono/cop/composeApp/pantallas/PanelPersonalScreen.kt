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
 * Vista del rol PERSONAL ENCARGADO.
 * COP-RN-12/13: revisa la agenda completa del Centro y las solicitudes por confirmar.
 */
@Composable
fun PanelPersonalScreen(usuario: Usuario, onCerrarSesion: () -> Unit) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    val porConfirmar = estado.citas.filter { it.estado == EstadoCita.PENDIENTE }
    val resto = estado.citas.filter { it.estado != EstadoCita.PENDIENTE }

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                porConfirmar.size to "Por confirmar",
                estado.citas.count { it.estado == EstadoCita.CONFIRMADA } to "Confirmadas",
                estado.citas.size to "Total"
            )
        }

        item { TituloSeccion("Solicitudes por confirmar") }
        if (porConfirmar.isEmpty()) {
            item { MensajeVacio("No hay solicitudes pendientes.") }
        } else {
            items(porConfirmar, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = true)
            }
        }

        item { TituloSeccion("Agenda del Centro") }
        if (resto.isEmpty()) {
            item { MensajeVacio("No hay más citas registradas.") }
        } else {
            items(resto, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = true)
            }
        }
    }
}
