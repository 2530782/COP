package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.Cargando
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.ui.NotificacionesViewModel

/** COP-RN-11: avisos cuando el Centro confirma, modifica o cancela una cita. */
@Composable
fun NotificacionesScreen(
    onCambioSinLeer: (Int) -> Unit,
    onRegresar: () -> Unit,
    vm: NotificacionesViewModel = viewModel { NotificacionesViewModel() }
) {
    val estado by vm.estado.collectAsState()

    // Cada vez que se abre la pantalla se vuelven a pedir los avisos
    LaunchedEffect(Unit) {
        vm.cargar()
    }

    LaunchedEffect(estado.avisos) {
        onCambioSinLeer(estado.avisos.count { !it.leida })
    }

    PantallaSimple(
        titulo = "Avisos del Centro",
        onRegresar = onRegresar,
        accion = {
            TextButton(onClick = vm::marcarTodas) {
                Text("Marcar leídos", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) {
        estado.error?.let { Aviso(it, esError = true) }

        if (estado.cargando) {
            Cargando()
        } else if (estado.avisos.isEmpty()) {
            MensajeVacio("No tienes avisos.")
        }

        for (aviso in estado.avisos) {
            val fondo = if (aviso.leida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer

            Card(
                onClick = { vm.marcarLeida(aviso) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = fondo)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = (if (aviso.leida) "" else "● ") + aviso.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(aviso.mensaje)
                    Text(
                        text = FechasApp.momento(aviso.creado),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
