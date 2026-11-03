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
import com.poligono.cop.composeApp.componentes.TarjetaServicio
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol USUARIO (paciente).
 * COP-RN-03/04: ve los servicios y quién los atiende.
 * COP-RN-06/07: ve sus citas y su estado.
 */
@Composable
fun PanelUsuarioScreen(usuario: Usuario, onCerrarSesion: () -> Unit) {
    // key = un ViewModel distinto por usuario (si cambia de cuenta, no reutiliza datos viejos)
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    val activas = estado.citas.filter {
        it.estado == EstadoCita.PENDIENTE || it.estado == EstadoCita.CONFIRMADA
    }
    val historial = estado.citas - activas.toSet()

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                activas.size to "Próximas",
                estado.citas.count { it.estado == EstadoCita.ATENDIDA } to "Atendidas",
                estado.servicios.count { it.activo } to "Servicios"
            )
        }

        item { TituloSeccion("Mis próximas citas") }
        if (activas.isEmpty()) {
            item { MensajeVacio("No tienes citas próximas.") }
        } else {
            items(activas, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = false, mostrarProfesional = true)
            }
        }

        if (historial.isNotEmpty()) {
            item { TituloSeccion("Historial") }
            items(historial, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = false, mostrarProfesional = true)
            }
        }

        item { TituloSeccion("Servicios del Centro") }
        items(estado.servicios.filter { it.activo }, key = { it.id }) { servicio ->
            TarjetaServicio(
                servicio = servicio,
                profesionales = estado.profesionales
                    .filter { it.servicioId == servicio.id }
                    .map { it.nombre }
            )
        }
    }
}
