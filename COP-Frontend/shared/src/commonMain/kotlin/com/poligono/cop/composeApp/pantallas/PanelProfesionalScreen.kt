package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.BotonMenu
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.TarjetaCita
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol PROFESIONAL (fisioterapeuta, nutrióloga, psicólogo).
 * Solo ve SU agenda, con el nombre de cada paciente,
 * y marca cada cita como atendida o inasistencia (COP-RN-18).
 */
@Composable
fun PanelProfesionalScreen(
    usuario: Usuario,
    onCerrarSesion: () -> Unit,
    onNavegar: (Pantalla) -> Unit
) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()
    var accion by remember { mutableStateOf<AccionCita?>(null) }

    LaunchedEffect(Unit) {
        vm.cargar()
    }

    val hoy = FechasApp.hoy()
    val deHoy = estado.citas.filter { it.fecha == hoy }
    val proximas = estado.citas.filter { it.fecha > hoy && it.estado.activa }
    val anteriores = estado.citas.filter { it.fecha < hoy }.reversed()

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                deHoy.count { it.estado.activa } to "Hoy",
                estado.citas.count { it.estado == EstadoCita.PENDIENTE } to "Por confirmar",
                estado.citas.count { it.estado == EstadoCita.ATENDIDA } to "Atendidas"
            )
        }

        item { BotonMenu("📍", "Polígonos en el mapa (GPS)", onClick = { onNavegar(Pantalla.Poligonos) }) }

        item { TituloSeccion("Mi agenda de hoy") }
        if (deHoy.isEmpty()) {
            item { MensajeVacio("No tienes citas hoy.") }
        } else {
            items(deHoy, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = false) {
                    BotonesAgenda(cita, esPersonal = false, onAccion = { accion = it }, onModificar = {})
                }
            }
        }

        item { TituloSeccion("Próximas citas") }
        if (proximas.isEmpty()) {
            item { MensajeVacio("No tienes citas próximas.") }
        } else {
            items(proximas, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = false)
            }
        }

        if (anteriores.isNotEmpty()) {
            item { TituloSeccion("Citas anteriores") }
            items(anteriores, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = false) {
                    BotonesAgenda(cita, esPersonal = false, onAccion = { accion = it }, onModificar = {})
                }
            }
        }
    }

    accion?.let { a ->
        DialogoAccion(
            accion = a,
            onConfirmar = { motivo ->
                accion = null
                vm.cambiarEstado(a.cita, a.estado, motivo)
            },
            onCancelar = { accion = null }
        )
    }
}
