package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.BotonMenu
import com.poligono.cop.composeApp.componentes.DialogoConfirmar
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.TarjetaCita
import com.poligono.cop.composeApp.componentes.TarjetaServicio
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.data.local.Avisos
import com.poligono.cop.data.local.PoligonosRespaldo
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Configuracion
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.plataforma.agregarAlCalendario
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol USUARIO (paciente).
 * COP-RN-03/04: ve los servicios y quién los atiende.
 * COP-RN-05: agenda una cita.  COP-RN-06/07: ve sus citas y su estado.
 * COP-RN-08/09: cancela o reprograma.  COP-RNN-07: recordatorio dentro de la app.
 */
@Composable
fun PanelUsuarioScreen(
    usuario: Usuario,
    configuracion: Configuracion,
    avisosSinLeer: Int,
    onCerrarSesion: () -> Unit,
    onNavegar: (Pantalla) -> Unit
) {
    // key = un ViewModel distinto por usuario (si cambia de cuenta, no reutiliza datos viejos)
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()
    var porCancelar by remember { mutableStateOf<Cita?>(null) }

    // Cada vez que aparece la pantalla (también al regresar de agendar) se actualiza
    LaunchedEffect(Unit) {
        vm.cargar()
    }

    val hoy = FechasApp.hoy()
    val activas = estado.citas.filter { it.estado.activa && it.fecha >= hoy }
    val historial = estado.citas - activas.toSet()
    val proximas24h = Avisos.citasProximas(estado.citas)

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        // COP-RNN-07: recordatorio visible aunque las notificaciones estén apagadas
        items(proximas24h, key = { "recordatorio-${it.id}" }) { cita ->
            Aviso("⏰ Recordatorio: ${cita.servicio} el ${FechasApp.larga(cita.fecha)} a las ${cita.hora}.")
        }

        item {
            FilaResumen(
                activas.size to "Próximas",
                estado.citas.count { it.estado == EstadoCita.ATENDIDA } to "Atendidas",
                avisosSinLeer to "Avisos nuevos"
            )
        }

        item {
            BotonMenu("📅", "Agendar una cita", onClick = { onNavegar(Pantalla.Agendar(usuario)) })
        }
        item {
            BotonMenu(
                icono = "🔔",
                texto = "Avisos del Centro",
                onClick = { onNavegar(Pantalla.Notificaciones(usuario)) },
                insignia = avisosSinLeer
            )
        }
        item {
            BotonMenu("📍", "Polígonos cercanos (GPS)", onClick = { onNavegar(Pantalla.Poligonos) })
        }
        item {
            BotonMenu("❓", "Ayuda y contacto", onClick = { onNavegar(Pantalla.Ayuda) })
        }

        item { TituloSeccion("Mis próximas citas") }
        if (activas.isEmpty()) {
            item { MensajeVacio("No tienes citas próximas.") }
        } else {
            items(activas, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = false, mostrarProfesional = true) {
                    AccionesCitaUsuario(
                        cita = cita,
                        configuracion = configuracion,
                        sinConexion = estado.sinConexion != null,
                        onCancelar = { porCancelar = cita },
                        onReprogramar = { onNavegar(Pantalla.Reprogramar(usuario, cita)) },
                        onAviso = vm::mostrarMensaje
                    )
                }
            }
        }

        if (historial.isNotEmpty()) {
            item { TituloSeccion("Historial") }
            items(historial, key = { it.id }) { cita ->
                TarjetaCita(cita, mostrarPaciente = false, mostrarProfesional = true) {
                    // COP-RNN-05: si el Centro canceló o no aceptó, se ofrece agendar de nuevo
                    if (cita.estado == EstadoCita.CANCELADA || cita.estado == EstadoCita.RECHAZADA) {
                        TextButton(onClick = { onNavegar(Pantalla.Agendar(usuario, servicioId = cita.servicioId)) }) {
                            Text("Agendar de nuevo")
                        }
                    }
                }
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

    porCancelar?.let { cita ->
        DialogoConfirmar(
            titulo = "¿Cancelar la cita?",
            texto = "${cita.servicio}, ${FechasApp.larga(cita.fecha)} a las ${cita.hora}. El horario quedará libre para otra persona.",
            textoConfirmar = "Sí, cancelar",
            pedirMotivo = false,
            onConfirmar = {
                porCancelar = null
                vm.cancelar(cita)
            },
            onCancelar = { porCancelar = null }
        )
    }
}

@Composable
private fun AccionesCitaUsuario(
    cita: Cita,
    configuracion: Configuracion,
    sinConexion: Boolean,
    onCancelar: () -> Unit,
    onReprogramar: () -> Unit,
    onAviso: (String) -> Unit
) {
    if (cita.puedeModificar && !sinConexion) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReprogramar) {
                Text("Reprogramar")
            }
            OutlinedButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    } else if (!cita.puedeModificar) {
        // COP-RNN-04: fuera de plazo se explica el motivo
        Text(
            text = "Faltan menos de ${configuracion.plazoHorasCambio} h: para cambios llama al Centro (${configuracion.telefonoCentro}).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // COP-RI-04: agregar la cita confirmada al calendario del teléfono
    if (cita.estado == EstadoCita.CONFIRMADA) {
        TextButton(
            onClick = {
                val inicio = FechasApp.millisDe(cita.fecha, cita.hora)
                val ok = agregarAlCalendario(
                    titulo = "Cita de ${cita.servicio} (COP)",
                    descripcion = "Con ${cita.profesional}. Centro de Desarrollo Comunitario Polígono Guadalupe.",
                    ubicacion = PoligonosRespaldo.principal.direccion,
                    inicioMillis = inicio,
                    finMillis = inicio + 60 * 60_000L
                )

                if (!ok) {
                    onAviso("No se encontró una app de calendario en el teléfono.")
                }
            }
        ) {
            Text("📆 Agregar a mi calendario")
        }
    }
}
