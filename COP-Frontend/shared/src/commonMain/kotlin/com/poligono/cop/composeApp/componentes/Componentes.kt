package com.poligono.cop.composeApp.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario

/*
 * Piezas visuales que se repiten en varias pantallas.
 * Hacerlas una sola vez = mismo estilo en toda la app (COP-RC-10).
 */

/** Barra superior de cada panel: iniciales, nombre, rol y botón para salir. */
@Composable
fun EncabezadoPanel(usuario: Usuario, onCerrarSesion: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()   // no se encima con la barra de estado del teléfono
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo con las iniciales del usuario
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciales(usuario.nombre),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = usuario.rol.etiqueta,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onCerrarSesion) {
                Text("Salir", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** Título de cada sección dentro de un panel. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 8.dp)
    )
}

/** Tarjeta pequeña con un número grande (ej. "3  Pendientes"). */
@Composable
fun TarjetaResumen(numero: Int, texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Fila de 3 tarjetas de resumen del mismo ancho. */
@Composable
fun FilaResumen(vararg datos: Pair<Int, String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        datos.forEach { (numero, texto) ->
            TarjetaResumen(numero, texto, Modifier.weight(1f))
        }
    }
}

/**
 * Tarjeta de una cita (COP-RN-06: servicio, fecha, horario y estado).
 * Según quién la vea se muestra el paciente, el profesional o ambos.
 */
@Composable
fun TarjetaCita(
    cita: Cita,
    mostrarPaciente: Boolean,
    mostrarProfesional: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cita.servicio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                EtiquetaEstado(cita.estado)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatearFecha(cita.fecha)} · ${cita.hora} h",
                style = MaterialTheme.typography.bodyLarge
            )
            if (mostrarProfesional) {
                Text(
                    text = "Atiende: ${cita.profesional}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mostrarPaciente) {
                Text(
                    text = "Paciente: ${cita.paciente}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Etiqueta de color con el estado. Siempre lleva TEXTO, no solo color (accesibilidad). */
@Composable
fun EtiquetaEstado(estado: EstadoCita) {
    val (fondo, texto) = when (estado) {
        EstadoCita.PENDIENTE -> Color(0xFFFFF1C2) to Color(0xFF6B4E00)
        EstadoCita.CONFIRMADA -> Color(0xFFD6F2DD) to Color(0xFF1B5E2B)
        EstadoCita.CANCELADA -> Color(0xFFFADBD8) to Color(0xFF8C1D18)
        EstadoCita.ATENDIDA -> Color(0xFFE1E6EC) to Color(0xFF3A4A5C)
    }
    Text(
        text = estado.etiqueta,
        color = texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Tarjeta de un servicio del Centro (COP-RN-03 y COP-RN-04). */
@Composable
fun TarjetaServicio(servicio: Servicio, profesionales: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = servicio.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${servicio.duracionMinutos} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = servicio.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (profesionales.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Profesionales: ${profesionales.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!servicio.activo) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Servicio inactivo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Mensaje gris para cuando una lista está vacía. */
@Composable
fun MensajeVacio(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}

/** Recuadro de error con botón para volver a intentar. */
@Composable
fun MensajeError(texto: String, onReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = texto, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}

// ---------- Funciones de apoyo (no dibujan nada) ----------

/** "Ana López" -> "AL",  "Lic. Carlos Ortiz" -> "CO" (se ignoran títulos como "Lic.") */
fun iniciales(nombre: String): String =
    nombre.split(" ")
        .filter { it.isNotBlank() && it.first().isLetter() && !it.endsWith(".") }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/** "2026-10-12" -> "12/10/2026" */
fun formatearFecha(fecha: String): String {
    val partes = fecha.split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else fecha
}
