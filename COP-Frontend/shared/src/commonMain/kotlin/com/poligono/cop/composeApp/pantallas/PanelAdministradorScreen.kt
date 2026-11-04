package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.BotonMenu
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.TarjetaServicio
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol ADMINISTRADOR.
 * COP-RN-17: administra servicios y profesionales.   COP-RN-14: horarios.
 * COP-RI-07: exporta la agenda.   COP-RS-07: revisa reportes de fallas.
 * COP-RE-05: resumen general (base para estadísticas futuras).
 */
@Composable
fun PanelAdministradorScreen(
    usuario: Usuario,
    onCerrarSesion: () -> Unit,
    onNavegar: (Pantalla) -> Unit
) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    LaunchedEffect(Unit) {
        vm.cargar()
    }

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                estado.servicios.count { it.activo } to "Servicios activos",
                estado.profesionales.size to "Profesionales",
                estado.citas.count { it.estado == EstadoCita.PENDIENTE } to "Por confirmar"
            )
        }

        item { BotonMenu("📅", "Agenda del Centro", onClick = { onNavegar(Pantalla.Agenda(usuario)) }) }
        item { BotonMenu("🩺", "Servicios y profesionales", onClick = { onNavegar(Pantalla.Catalogo(usuario)) }) }
        item { BotonMenu("🕘", "Horarios de atención", onClick = { onNavegar(Pantalla.Horarios(usuario)) }) }
        item {
            BotonMenu("☎️", "Registrar cita presencial o telefónica", onClick = {
                onNavegar(Pantalla.Agendar(usuario, manual = true))
            })
        }
        item { BotonMenu("📄", "Exportar agenda (PDF / Excel)", onClick = { onNavegar(Pantalla.Exportar(usuario)) }) }
        item { BotonMenu("🛠️", "Reportes de fallas", onClick = { onNavegar(Pantalla.Reportes(usuario)) }) }
        item { BotonMenu("📍", "Polígonos en el mapa (GPS)", onClick = { onNavegar(Pantalla.Poligonos) }) }

        item { TituloSeccion("Profesionales") }
        items(estado.profesionales, key = { it.username }) { profesional ->
            val servicio = estado.servicios.firstOrNull { it.id == profesional.servicioId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(profesional.nombre, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${servicio?.nombre ?: "Sin servicio"} · usuario: ${profesional.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { TituloSeccion("Catálogo de servicios") }
        items(estado.servicios, key = { it.id }) { servicio ->
            TarjetaServicio(
                servicio = servicio,
                profesionales = estado.profesionales
                    .filter { it.servicioId == servicio.id }
                    .map { it.nombre }
            )
        }
    }
}
