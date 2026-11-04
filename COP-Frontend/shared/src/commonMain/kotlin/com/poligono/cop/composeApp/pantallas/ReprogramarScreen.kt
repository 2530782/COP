package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.ui.ReprogramarViewModel

/**
 * COP-RN-09: el paciente reprograma su cita (vuelve a quedar pendiente).
 * COP-RN-16: el personal la modifica y se le avisa al paciente.
 */
@Composable
fun ReprogramarScreen(
    pantalla: Pantalla.Reprogramar,
    onTerminar: () -> Unit,
    onRegresar: () -> Unit
) {
    val vm: ReprogramarViewModel = viewModel(key = "reprogramar-${pantalla.visita}") {
        ReprogramarViewModel(pantalla.cita)
    }
    val estado by vm.estado.collectAsState()
    val esPersonal = pantalla.usuario.rol != Rol.USUARIO

    LaunchedEffect(estado.listo) {
        if (estado.listo) {
            onTerminar()
        }
    }

    PantallaSimple(
        titulo = if (esPersonal) "Modificar cita" else "Reprogramar cita",
        onRegresar = onRegresar
    ) {
        TituloSeccion("Cita actual")
        TarjetaCita(pantalla.cita, mostrarPaciente = esPersonal, mostrarProfesional = true)

        TituloSeccion("Nuevo día")
        if (estado.cargando) {
            Cargando()
        }
        ChipsFechas(estado.fechas, estado.fecha, vm::elegirFecha)

        if (estado.fecha != null) {
            TituloSeccion("Nueva hora")
            Fichas(estado.horas, estado.hora, vm::elegirHora)
        }

        if (esPersonal) {
            CampoTexto(estado.motivo, vm::onMotivoChange, "Motivo del cambio (se le avisa al paciente)")
        } else {
            Text("Tu cita volverá a quedar pendiente hasta que el Centro la confirme.")
        }

        estado.error?.let { Aviso(it, esError = true) }

        Button(
            onClick = vm::guardar,
            enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar cambio")
        }
    }
}
