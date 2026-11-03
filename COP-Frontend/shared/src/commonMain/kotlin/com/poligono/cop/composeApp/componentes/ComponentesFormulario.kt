package com.poligono.cop.composeApp.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.poligono.cop.domain.model.FechaDisponible
import com.poligono.cop.domain.util.FechasApp

/*
 * Piezas para las pantallas de formularios y acciones.
 * Mismo estilo que Componentes.kt (COP-RC-10 coherencia, COP-RC-07 accesibilidad).
 */

/**
 * Esqueleto de las pantallas que se abren desde un panel (agendar, avisos, ayuda...):
 * barra superior con "Regresar" y el contenido con scroll.
 * Si [desplazable] es false, el contenido no se envuelve en scroll (pantalla del mapa).
 */
@Composable
fun PantallaSimple(
    titulo: String,
    onRegresar: () -> Unit,
    desplazable: Boolean = true,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRegresar) {
                        Text("← Regresar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(
                        text = titulo,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    accion?.invoke()
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { espacioInterno ->
        val base = Modifier
            .fillMaxSize()
            .padding(espacioInterno)

        Column(
            modifier = if (desplazable) {
                base
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            } else {
                base
            },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
    }
}

/** Recuadro de aviso (informativo, éxito o advertencia). */
@Composable
fun Aviso(texto: String, esError: Boolean = false) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Text(
        text = texto,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo, RoundedCornerShape(10.dp))
            .padding(12.dp)
    )
}

@Composable
fun Cargando(texto: String = "Cargando…") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(texto)
    }
}

/** Botón grande del menú de cada panel (fácil de tocar). */
@Composable
fun BotonMenu(
    icono: String,
    texto: String,
    onClick: () -> Unit,
    insignia: Int = 0
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icono, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (insignia > 0) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = insignia.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
    lineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Lista desplegable sencilla. Con [permitirTodos] aparece la opción "Todos" (= null). */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    texto: (T) -> String,
    onElegir: (T?) -> Unit,
    permitirTodos: Boolean = false
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { abierto = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            val actual = if (seleccion != null) texto(seleccion) else if (permitirTodos) "Todos" else "Elegir"
            Text("$etiqueta: $actual  ▾")
        }

        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (permitirTodos) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = {
                        abierto = false
                        onElegir(null)
                    }
                )
            }
            for (opcion in opciones) {
                DropdownMenuItem(
                    text = { Text(texto(opcion)) },
                    onClick = {
                        abierto = false
                        onElegir(opcion)
                    }
                )
            }
        }
    }
}

/** Días disponibles como fichas: "lun 9 nov (5)". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsFechas(
    fechas: List<FechaDisponible>,
    seleccion: String?,
    onElegir: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (f in fechas) {
            FilterChip(
                selected = f.fecha == seleccion,
                onClick = { onElegir(f.fecha) },
                label = { Text("${FechasApp.corta(f.fecha)} (${f.espaciosLibres})") }
            )
        }
    }
}

/** Fichas de texto (horas, pestañas, filtros). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Fichas(
    opciones: List<String>,
    seleccion: String?,
    onElegir: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (opcion in opciones) {
            FilterChip(
                selected = opcion == seleccion,
                onClick = { onElegir(opcion) },
                label = { Text(opcion) }
            )
        }
    }
}

/** Diálogo para confirmar una acción; con [pedirMotivo] se escribe un motivo para el usuario. */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    pedirMotivo: Boolean,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(texto)
                if (pedirMotivo) {
                    CampoTexto(
                        valor = motivo,
                        onCambio = { motivo = it },
                        etiqueta = "Motivo (se le avisa al usuario)"
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(motivo) }) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Regresar")
            }
        }
    )
}

/** Texto base del aviso de privacidad (COP-RC-05). Debe revisarlo y aprobarlo el Centro. */
const val AVISO_PRIVACIDAD = """El Centro de Desarrollo Comunitario Polígono Guadalupe (Tulancingo, Hgo.) es responsable del tratamiento de tus datos personales.

Datos que se recaban: nombre, teléfono, correo electrónico (opcional) y la información de tus citas (servicio, fecha, hora y estado).

Finalidad: agendar, confirmar, recordar, reprogramar y cancelar tus citas de fisioterapia, nutrición, psicología y otros servicios del Centro, y avisarte de cambios.

Los datos de tus citas pueden referirse a servicios de salud; solo tú y el personal autorizado del Centro pueden consultarlos. No se comparten con terceros, salvo obligación legal.

Puedes ejercer tus derechos de acceso, rectificación, cancelación y oposición (ARCO) acudiendo al Centro o llamando al 775 755 8450 ext. 1135.

Ubicación: la app usa el GPS solo mientras ves el mapa de polígonos, para mostrarte cuál te queda más cerca. Tu ubicación no se envía ni se guarda en el servidor.

Huella: si eliges entrar con huella, la huella nunca sale de tu teléfono; el sistema solo le confirma a la app que eres tú.

Este aviso se emite conforme a la Ley Federal de Protección de Datos Personales en Posesión de los Particulares."""

@Composable
fun DialogoAvisoPrivacidad(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Aviso de privacidad") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(AVISO_PRIVACIDAD)
            }
        },
        confirmButton = {
            Button(onClick = onCerrar) {
                Text("Entendido")
            }
        }
    )
}
