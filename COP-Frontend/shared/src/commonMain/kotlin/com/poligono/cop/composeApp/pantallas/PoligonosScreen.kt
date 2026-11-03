package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.sensores.EstadoGps
import com.poligono.cop.composeApp.sensores.MapaPoligonos
import com.poligono.cop.composeApp.sensores.rememberSensorUbicacion
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Distancias
import com.poligono.cop.plataforma.abrirComoLlegar
import com.poligono.cop.plataforma.llamarTelefono
import com.poligono.cop.ui.PoligonosViewModel

/**
 * SENSOR GPS: muestra en el mapa los polígonos disponibles en Tulancingo,
 * resalta el Polígono Guadalupe (donde se atienden las citas) y dice
 * cuál le queda más cerca a la persona. Se puede abrir sin iniciar sesión.
 */
@Composable
fun PoligonosScreen(
    onRegresar: () -> Unit,
    vm: PoligonosViewModel = viewModel { PoligonosViewModel() }
) {
    val estado by vm.estado.collectAsState()

    // 1-3. Habilitación, calibración y obtención (composeApp/sensores)
    val gps = rememberSensorUbicacion(activo = true)
    val ubicacion = gps.ubicacion

    // 4. Procesamiento: distancia a cada polígono, del más cercano al más lejano
    val ordenados = vm.ordenarPorDistancia(ubicacion)
    val principal = ordenados.firstOrNull { it.poligono.esPrincipal }
    val masCercano = ordenados.firstOrNull()?.takeIf { it.metros != null }

    PantallaSimple(
        titulo = "Polígonos en Tulancingo",
        onRegresar = onRegresar,
        desplazable = false
    ) {
        MapaPoligonos(
            poligonos = estado.poligonos,
            ubicacion = ubicacion,
            seleccionadoId = estado.seleccionadoId,
            enfoque = estado.enfoque,
            onSeleccionar = vm::seleccionar,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔴 Polígono Guadalupe   🔵 Otros polígonos   🟡 Seleccionado   ● Tú",
                style = MaterialTheme.typography.bodySmall
            )

            EstadoDelGps(gps)

            if (estado.sinConexion) {
                Aviso("Sin conexión: se muestran los datos guardados en el teléfono.", esError = true)
            }

            if (masCercano != null) {
                val metros = masCercano.metros ?: 0.0
                Aviso(
                    "Te queda más cerca: ${masCercano.poligono.nombre}, a ${Distancias.texto(metros)} " +
                        "(unos ${Distancias.minutosCaminando(metros)} min caminando)."
                )
            }

            if (principal != null) {
                TituloSeccion("Donde se atienden tus citas")
                TarjetaPoligono(
                    poligono = principal.poligono,
                    metros = principal.metros,
                    resaltado = true,
                    onVerEnMapa = { vm.verEnMapa(principal.poligono) }
                )
            }

            TituloSeccion("Otros polígonos disponibles")
            for (item in ordenados) {
                if (!item.poligono.esPrincipal) {
                    TarjetaPoligono(
                        poligono = item.poligono,
                        metros = item.metros,
                        resaltado = item.poligono.id == estado.seleccionadoId,
                        onVerEnMapa = { vm.verEnMapa(item.poligono) }
                    )
                }
            }

            Text(
                text = "Mapa: © colaboradores de OpenStreetMap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

/** 1. Habilitación del sensor: permiso de ubicación y GPS encendido. */
@Composable
private fun EstadoDelGps(gps: EstadoGps) {
    val ubicacion = gps.ubicacion

    when {
        !gps.permisoConcedido -> {
            Aviso("Activa tu ubicación para saber qué polígono te queda más cerca.")
            Button(onClick = gps.solicitarPermiso, modifier = Modifier.fillMaxWidth()) {
                Text("📍 Permitir ubicación")
            }
        }

        !gps.gpsEncendido -> {
            Aviso("El GPS del teléfono está apagado.", esError = true)
            OutlinedButton(onClick = gps.abrirAjustesUbicacion, modifier = Modifier.fillMaxWidth()) {
                Text("Encender ubicación")
            }
        }

        ubicacion == null -> {
            Aviso("Buscando señal GPS… Si estás dentro de un edificio, acércate a una ventana.")
        }

        else -> {
            Text(
                text = "📡 Ubicación encontrada (precisión ±${ubicacion.precisionMetros.toInt()} m)",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TarjetaPoligono(
    poligono: Poligono,
    metros: Double?,
    resaltado: Boolean,
    onVerEnMapa: () -> Unit
) {
    val fondo = if (resaltado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(poligono.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (metros != null) {
                Text(
                    text = "🚶 A ${Distancias.texto(metros)} de ti",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text("📍 ${poligono.direccion}")
            Text("🕘 ${poligono.horario}")
            Text("☎️ ${poligono.telefono}")
            Text("🧩 ${poligono.servicios}")

            if (poligono.ubicacionAproximada) {
                Text(
                    text = "La ubicación en el mapa es aproximada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { abrirComoLlegar(poligono.latitud, poligono.longitud, poligono.nombre) }) {
                    Text("Cómo llegar")
                }
                OutlinedButton(onClick = onVerEnMapa) {
                    Text("Ver en mapa")
                }
            }

            TextButton(onClick = { llamarTelefono(poligono.telefono) }) {
                Text("Llamar")
            }
        }
    }
}
