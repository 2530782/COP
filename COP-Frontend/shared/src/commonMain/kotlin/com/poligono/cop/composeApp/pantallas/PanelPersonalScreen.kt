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
import com.poligono.cop.composeApp.componentes.Fichas
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.Selector
import com.poligono.cop.composeApp.componentes.TarjetaCita
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.ui.PanelViewModel

private const val HOY = "Hoy"
private const val MANANA = "Mañana"
private const val PROXIMAS = "Próximas"
private const val TODAS = "Todas"

/**
 * Vista del rol PERSONAL ENCARGADO (y agenda del administrador).
 * COP-RN-12: confirma o rechaza solicitudes.   COP-RN-13: agenda por servicio, profesional y fecha.
 * COP-RN-15: cita presencial o telefónica.     COP-RN-16: modifica o cancela.
 * COP-RN-18: atendida o inasistencia.
 */
@Composable
fun PanelPersonalScreen(
    usuario: Usuario,
    onCerrarSesion: () -> Unit,
    onNavegar: (Pantalla) -> Unit,
    onRegresar: (() -> Unit)? = null
) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    var periodo by remember { mutableStateOf(PROXIMAS) }
    var servicio by remember { mutableStateOf<Servicio?>(null) }
    var profesional by remember { mutableStateOf<Profesional?>(null) }
    var accion by remember { mutableStateOf<AccionCita?>(null) }

    LaunchedEffect(Unit) {
        vm.cargar()
    }

    val hoy = FechasApp.hoy()
    val manana = FechasApp.sumarDias(hoy, 1)

    val porConfirmar = estado.citas.filter { it.estado == EstadoCita.PENDIENTE && it.fecha >= hoy }

    // COP-RN-13: filtros de la agenda
    val agenda = estado.citas
        .filter {
            when (periodo) {
                HOY -> it.fecha == hoy
                MANANA -> it.fecha == manana
                PROXIMAS -> it.fecha >= hoy
                else -> true
            }
        }
        .filter { servicio == null || it.servicioId == servicio?.id }
        .filter { profesional == null || it.profesionalUsername == profesional?.username }

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion, onRegresar = onRegresar) {
        item {
            FilaResumen(
                porConfirmar.size to "Por confirmar",
                estado.citas.count { it.fecha == hoy && it.estado.activa } to "Hoy",
                estado.citas.count { it.estado == EstadoCita.CONFIRMADA && it.fecha >= hoy } to "Confirmadas"
            )
        }

        if (onRegresar == null) {
            item {
                BotonMenu("☎️", "Registrar cita presencial o telefónica", onClick = {
                    onNavegar(Pantalla.Agendar(usuario, manual = true))
                })
            }
            item { BotonMenu("🕘", "Horarios de atención", onClick = { onNavegar(Pantalla.Horarios(usuario)) }) }
            item { BotonMenu("📄", "Exportar agenda (PDF / Excel)", onClick = { onNavegar(Pantalla.Exportar(usuario)) }) }
            item { BotonMenu("📍", "Polígonos en el mapa (GPS)", onClick = { onNavegar(Pantalla.Poligonos) }) }
        }

        item { TituloSeccion("Solicitudes por confirmar") }
        if (porConfirmar.isEmpty()) {
            item { MensajeVacio("No hay solicitudes pendientes.") }
        } else {
            items(porConfirmar, key = { "pendiente-${it.id}" }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = true) {
                    BotonesAgenda(
                        cita = cita,
                        esPersonal = true,
                        onAccion = { accion = it },
                        onModificar = { onNavegar(Pantalla.Reprogramar(usuario, cita)) }
                    )
                }
            }
        }

        item { TituloSeccion("Agenda del Centro") }
        item {
            Fichas(
                opciones = listOf(HOY, MANANA, PROXIMAS, TODAS),
                seleccion = periodo,
                onElegir = { periodo = it }
            )
        }
        item {
            Selector(
                etiqueta = "Servicio",
                opciones = estado.servicios,
                seleccion = servicio,
                texto = { it.nombre },
                onElegir = {
                    servicio = it
                    profesional = null
                },
                permitirTodos = true
            )
        }
        item {
            Selector(
                etiqueta = "Profesional",
                opciones = estado.profesionales.filter { servicio == null || it.servicioId == servicio?.id },
                seleccion = profesional,
                texto = { it.nombre },
                onElegir = { profesional = it },
                permitirTodos = true
            )
        }

        if (agenda.isEmpty()) {
            item { MensajeVacio("No hay citas con estos filtros.") }
        } else {
            items(agenda, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = true, mostrarProfesional = true) {
                    if (cita.estado != EstadoCita.PENDIENTE) {
                        BotonesAgenda(
                            cita = cita,
                            esPersonal = true,
                            onAccion = { accion = it },
                            onModificar = { onNavegar(Pantalla.Reprogramar(usuario, cita)) }
                        )
                    }
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
