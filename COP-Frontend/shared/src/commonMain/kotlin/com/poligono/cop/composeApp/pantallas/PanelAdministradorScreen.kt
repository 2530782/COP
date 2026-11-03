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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.FilaResumen
import com.poligono.cop.composeApp.componentes.PanelBase
import com.poligono.cop.composeApp.componentes.TarjetaServicio
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.PanelViewModel

/**
 * Vista del rol ADMINISTRADOR.
 * COP-RN-17: consulta servicios y profesionales del Centro.
 * COP-RE-05: resumen general (base para estadísticas futuras).
 */
@Composable
fun PanelAdministradorScreen(usuario: Usuario, onCerrarSesion: () -> Unit) {
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.username}") { PanelViewModel(usuario) }
    val estado by vm.estado.collectAsState()

    PanelBase(usuario, estado, onActualizar = vm::cargar, onCerrarSesion = onCerrarSesion) {
        item {
            FilaResumen(
                estado.servicios.count { it.activo } to "Servicios activos",
                estado.profesionales.size to "Profesionales",
                estado.citas.size to "Citas registradas"
            )
        }

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
