package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.CampoTexto
import com.poligono.cop.composeApp.componentes.Cargando
import com.poligono.cop.composeApp.componentes.ChipsFechas
import com.poligono.cop.composeApp.componentes.Fichas
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.composeApp.componentes.TarjetaCita
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.data.local.PoligonosRespaldo
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.plataforma.ManejarAtras
import com.poligono.cop.plataforma.abrirComoLlegar
import com.poligono.cop.ui.AgendarViewModel

/**
 * Agendar una cita en máximo 5 pasos desde el panel (COP-RC-06):
 * 1 servicio · 2 profesional · 3 día · 4 hora · 5 confirmar.
 * Con pantalla.manual = true el personal registra una cita presencial o telefónica (COP-RN-15).
 */
@Composable
fun AgendarScreen(
    pantalla: Pantalla.Agendar,
    onTerminar: () -> Unit,
    onRegresar: () -> Unit
) {
    val vm: AgendarViewModel = viewModel(key = "agendar-${pantalla.visita}") {
        AgendarViewModel(manual = pantalla.manual, servicioInicial = pantalla.servicioId)
    }
    val estado by vm.estado.collectAsState()

    // El botón "atrás" del teléfono primero regresa al paso anterior
    ManejarAtras(habilitado = estado.paso > 1 && estado.citaCreada == null) {
        vm.pasoAnterior()
    }

    val titulo = when {
        estado.citaCreada != null -> "Listo"
        pantalla.manual -> "Cita presencial · paso ${estado.paso} de 5"
        else -> "Agendar · paso ${estado.paso} de 5"
    }

    PantallaSimple(
        titulo = titulo,
        onRegresar = {
            if (!vm.pasoAnterior()) {
                onRegresar()
            }
        }
    ) {
        val creada = estado.citaCreada

        if (creada != null) {
            if (pantalla.manual) {
                Aviso("✅ Cita registrada y confirmada.")
            } else {
                Aviso("✅ Tu solicitud quedó PENDIENTE. Te avisaremos cuando el Centro la confirme. Un día antes recibirás un recordatorio.")
            }

            TarjetaCita(creada, mostrarPaciente = pantalla.manual, mostrarProfesional = true)

            if (pantalla.manual) {
                Button(onClick = vm::otraCita, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar otra cita")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val p = PoligonosRespaldo.principal
                        abrirComoLlegar(p.latitud, p.longitud, p.nombre)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📍 Cómo llegar al Polígono Guadalupe")
                }
            }

            Button(onClick = onTerminar, modifier = Modifier.fillMaxWidth()) {
                Text("Terminar")
            }
            return@PantallaSimple
        }

        estado.error?.let { Aviso(it, esError = true) }

        if (estado.cargando) {
            Cargando()
        }

        when (estado.paso) {
            1 -> {
                TituloSeccion("1. ¿Qué servicio necesitas?")
                for (s in estado.servicios) {
                    Opcion(
                        titulo = s.nombre,
                        detalle = "${s.descripcion} (${s.duracionMinutos} min)",
                        onClick = { vm.elegirServicio(s) }
                    )
                }
            }

            2 -> {
                TituloSeccion("2. Elige al profesional de ${estado.servicio?.nombre ?: ""}")
                if (estado.profesionales.isEmpty() && !estado.cargando) {
                    Aviso("Por ahora no hay profesionales disponibles para este servicio.")
                }
                for (p in estado.profesionales) {
                    Opcion(
                        titulo = p.nombre,
                        detalle = estado.servicio?.nombre ?: "",
                        onClick = { vm.elegirProfesional(p) }
                    )
                }
            }

            3 -> {
                TituloSeccion("3. Elige el día")
                Text("Entre paréntesis: lugares libres ese día.")
                if (estado.fechas.isEmpty() && !estado.cargando) {
                    Aviso("No hay días disponibles en los próximos 30 días. Prueba con otro profesional.")
                }
                ChipsFechas(estado.fechas, estado.fecha, vm::elegirFecha)
            }

            4 -> {
                TituloSeccion("4. Elige la hora del ${estado.fecha?.let { FechasApp.larga(it) } ?: ""}")
                if (estado.horas.isEmpty()) {
                    Cargando("Buscando horarios…")
                }
                Fichas(estado.horas, estado.hora, vm::elegirHora)
            }

            else -> {
                TituloSeccion("5. Confirma la cita")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Servicio: ${estado.servicio?.nombre}")
                        Text("Profesional: ${estado.profesional?.nombre}")
                        Text("Día: ${estado.fecha?.let { FechasApp.larga(it) }}")
                        Text("Hora: ${estado.hora}")
                        Text("Lugar: Polígono Guadalupe")
                    }
                }

                if (pantalla.manual) {
                    CampoTexto(estado.nombreContacto, vm::onNombreChange, "Nombre de la persona")
                    CampoTexto(estado.telefonoContacto, vm::onTelefonoChange, "Teléfono (10 dígitos)", KeyboardType.Phone)
                }

                CampoTexto(estado.notas, vm::onNotasChange, "Comentario para el profesional (opcional)", lineas = 3)

                Button(
                    onClick = vm::confirmar,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (pantalla.manual) "Registrar cita" else "Solicitar cita")
                }
            }
        }
    }
}

@Composable
private fun Opcion(
    titulo: String,
    detalle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
