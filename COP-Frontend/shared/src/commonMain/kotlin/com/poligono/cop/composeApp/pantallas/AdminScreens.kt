package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.CampoTexto
import com.poligono.cop.composeApp.componentes.Cargando
import com.poligono.cop.composeApp.componentes.MensajeVacio
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.composeApp.componentes.Selector
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.util.FechasApp
import com.poligono.cop.plataforma.abrirEnlace
import com.poligono.cop.ui.CatalogoViewModel
import com.poligono.cop.ui.HorariosViewModel
import com.poligono.cop.ui.ReportesViewModel

// ---------- Servicios y profesionales (COP-RN-17, COP-RNN-05, COP-RE-01) ----------

@Composable
fun CatalogoScreen(
    pantalla: Pantalla.Catalogo,
    onRegresar: () -> Unit
) {
    val vm: CatalogoViewModel = viewModel(key = "catalogo-${pantalla.visita}") { CatalogoViewModel() }
    val estado by vm.estado.collectAsState()

    var editarServicio by remember { mutableStateOf<Servicio?>(null) }
    var nuevoServicio by remember { mutableStateOf(false) }
    var editarProfesional by remember { mutableStateOf<Profesional?>(null) }
    var nuevoProfesional by remember { mutableStateOf(false) }

    PantallaSimple(titulo = "Servicios y profesionales", onRegresar = onRegresar) {
        estado.error?.let { Aviso(it, esError = true) }
        estado.mensaje?.let { Aviso(it) }

        if (estado.cargando) {
            Cargando()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion("Servicios", Modifier.weight(1f))
            TextButton(onClick = { nuevoServicio = true }) {
                Text("+ Agregar")
            }
        }
        for (s in estado.servicios) {
            FilaCatalogo(
                titulo = "${s.nombre} (${s.id})",
                detalle = "${s.descripcion} · ${s.duracionMinutos} min",
                activo = s.activo,
                onEditar = { editarServicio = s }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TituloSeccion("Profesionales", Modifier.weight(1f))
            TextButton(onClick = { nuevoProfesional = true }) {
                Text("+ Agregar")
            }
        }
        for (p in estado.profesionales) {
            val servicio = estado.servicios.firstOrNull { it.id == p.servicioId }
            FilaCatalogo(
                titulo = p.nombre,
                detalle = "${servicio?.nombre ?: "Sin servicio"} · usuario: ${p.username}",
                activo = p.activo,
                onEditar = { editarProfesional = p }
            )
        }
    }

    if (nuevoServicio || editarServicio != null) {
        val actual = editarServicio

        DialogoServicio(
            inicial = actual,
            onGuardar = { servicio ->
                vm.guardarServicio(servicio, esNuevo = actual == null)
                nuevoServicio = false
                editarServicio = null
            },
            onCancelar = {
                nuevoServicio = false
                editarServicio = null
            }
        )
    }

    if (nuevoProfesional || editarProfesional != null) {
        val actual = editarProfesional

        DialogoProfesional(
            inicial = actual,
            servicios = estado.servicios.filter { it.activo },
            onGuardar = { username, password, nombre, servicioId, activo ->
                vm.guardarProfesional(username, password, nombre, servicioId, activo, actual)
                nuevoProfesional = false
                editarProfesional = null
            },
            onCancelar = {
                nuevoProfesional = false
                editarProfesional = null
            }
        )
    }
}

@Composable
private fun FilaCatalogo(
    titulo: String,
    detalle: String,
    activo: Boolean,
    onEditar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (activo) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo + if (activo) "" else " (inactivo)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onEditar) {
                Text("Editar")
            }
        }
    }
}

@Composable
private fun DialogoServicio(
    inicial: Servicio?,
    onGuardar: (Servicio) -> Unit,
    onCancelar: () -> Unit
) {
    var id by remember { mutableStateOf(inicial?.id ?: "") }
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(inicial?.descripcion ?: "") }
    var duracion by remember { mutableStateOf((inicial?.duracionMinutos ?: 45).toString()) }
    var activo by remember { mutableStateOf(inicial?.activo ?: true) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nuevo servicio" else "Editar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inicial == null) {
                    CampoTexto(id, { id = it.uppercase().take(10) }, "Clave (ej. TLE)")
                }
                CampoTexto(nombre, { nombre = it }, "Nombre")
                CampoTexto(descripcion, { descripcion = it }, "Descripción", lineas = 3)
                CampoTexto(duracion, { duracion = it.filter { c -> c.isDigit() }.take(3) }, "Minutos por cita", KeyboardType.Number)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activo, onCheckedChange = { activo = it })
                    Text("Activo (visible para los usuarios)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(
                        Servicio(
                            id = id.trim(),
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim(),
                            duracionMinutos = duracion.toIntOrNull() ?: 0,
                            activo = activo
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoProfesional(
    inicial: Profesional?,
    servicios: List<Servicio>,
    onGuardar: (username: String, password: String, nombre: String, servicioId: String?, activo: Boolean) -> Unit,
    onCancelar: () -> Unit
) {
    var username by remember { mutableStateOf(inicial?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var servicio by remember { mutableStateOf(servicios.firstOrNull { it.id == inicial?.servicioId }) }
    var activo by remember { mutableStateOf(inicial?.activo ?: true) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nuevo profesional" else "Editar profesional") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(nombre, { nombre = it }, "Nombre (ej. Lic. Carlos Ortiz)")
                if (inicial == null) {
                    CampoTexto(username, { username = it.trim() }, "Usuario para entrar")
                }
                CampoTexto(
                    password,
                    { password = it },
                    if (inicial == null) "Contraseña" else "Contraseña nueva (vacío = no cambiar)",
                    KeyboardType.Password,
                    esContrasena = true
                )
                Selector(
                    etiqueta = "Servicio",
                    opciones = servicios,
                    seleccion = servicio,
                    texto = { it.nombre },
                    onElegir = { servicio = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activo, onCheckedChange = { activo = it })
                    Text("Disponible")
                }
                if (inicial != null && inicial.activo && !activo) {
                    Text(
                        text = "Al desactivarlo se cancelan sus citas futuras y se avisa a cada paciente para que reprograme (COP-RNN-05).",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(username, password, nombre, servicio?.id, activo) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

// ---------- Horarios de atención (COP-RN-14) ----------

@Composable
fun HorariosScreen(
    pantalla: Pantalla.Horarios,
    onRegresar: () -> Unit
) {
    val vm: HorariosViewModel = viewModel(key = "horarios-${pantalla.visita}") { HorariosViewModel() }
    val estado by vm.estado.collectAsState()

    PantallaSimple(titulo = "Horarios de atención", onRegresar = onRegresar) {
        Selector(
            etiqueta = "Profesional",
            opciones = estado.profesionales,
            seleccion = estado.profesional,
            texto = { it.nombre },
            onElegir = { it?.let(vm::elegirProfesional) }
        )

        estado.error?.let { Aviso(it, esError = true) }
        estado.mensaje?.let { Aviso(it) }

        if (estado.profesional != null) {
            TituloSeccion("Horario semanal")
            Text(
                text = "Cada cita dura lo que indica su servicio (ej. fisioterapia 50 min).",
                style = MaterialTheme.typography.bodySmall
            )

            if (estado.horarios.isEmpty()) {
                MensajeVacio("Todavía no tiene horarios.")
            }

            for (h in estado.horarios) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${FechasApp.nombreDia(h.diaSemana)}: ${h.horaInicio} a ${h.horaFin}",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.eliminar(h) }) {
                        Text("Quitar")
                    }
                }
            }

            TituloSeccion("Agregar horario")
            Selector(
                etiqueta = "Día",
                opciones = (1..7).toList(),
                seleccion = estado.dia,
                texto = { FechasApp.nombreDia(it) },
                onElegir = { vm.onDiaChange(it ?: 1) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    CampoTexto(estado.inicio, vm::onInicioChange, "Inicio (HH:MM)")
                }
                Column(modifier = Modifier.weight(1f)) {
                    CampoTexto(estado.fin, vm::onFinChange, "Fin (HH:MM)")
                }
            }
            Button(onClick = vm::agregar, modifier = Modifier.fillMaxWidth()) {
                Text("Agregar horario")
            }
        }
    }
}

// ---------- Exportar agenda (COP-RI-07) ----------

@Composable
fun ExportarScreen(onRegresar: () -> Unit) {
    val repository = Dependencias.repository
    var desde by remember { mutableStateOf(FechasApp.hoy()) }
    var hasta by remember { mutableStateOf(FechasApp.sumarDias(FechasApp.hoy(), 30)) }
    var servicios by remember { mutableStateOf<List<Servicio>>(emptyList()) }
    var servicio by remember { mutableStateOf<Servicio?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.obtenerServicios().onSuccess { servicios = it }
    }

    fun descargar(tipo: String) {
        error = when {
            !FechasApp.esValida(desde) || !FechasApp.esValida(hasta) -> "Escribe las fechas como AAAA-MM-DD."
            desde > hasta -> "La fecha inicial debe ser antes de la final."
            else -> null
        }

        if (error == null && !abrirEnlace(repository.urlExportar(tipo, desde, hasta, servicio?.id))) {
            error = "No se pudo abrir el navegador."
        }
    }

    PantallaSimple(titulo = "Exportar agenda", onRegresar = onRegresar) {
        Text("Descarga la agenda de un periodo. El archivo se abre en el navegador del teléfono.")
        CampoTexto(desde, { desde = it.take(10) }, "Desde (AAAA-MM-DD)")
        CampoTexto(hasta, { hasta = it.take(10) }, "Hasta (AAAA-MM-DD)")
        Selector(
            etiqueta = "Servicio",
            opciones = servicios,
            seleccion = servicio,
            texto = { it.nombre },
            onElegir = { servicio = it },
            permitirTodos = true
        )

        error?.let { Aviso(it, esError = true) }

        Button(onClick = { descargar("pdf") }, modifier = Modifier.fillMaxWidth()) {
            Text("📄 Descargar PDF")
        }
        OutlinedButton(onClick = { descargar("csv") }, modifier = Modifier.fillMaxWidth()) {
            Text("📊 Descargar Excel (CSV)")
        }
    }
}

// ---------- Reportes de fallas (COP-RS-07) ----------

@Composable
fun ReportesScreen(
    pantalla: Pantalla.Reportes,
    onRegresar: () -> Unit
) {
    val vm: ReportesViewModel = viewModel(key = "reportes-${pantalla.visita}") { ReportesViewModel() }
    val estado by vm.estado.collectAsState()

    PantallaSimple(titulo = "Reportes de fallas", onRegresar = onRegresar) {
        estado.error?.let { Aviso(it, esError = true) }

        if (estado.cargando) {
            Cargando()
        } else if (estado.reportes.isEmpty()) {
            MensajeVacio("No hay reportes.")
        }

        for (r in estado.reportes) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${r.usuario} · ${FechasApp.momento(r.creado)}", fontWeight = FontWeight.SemiBold)
                    Text(r.descripcion)
                    Text(r.dispositivo, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
