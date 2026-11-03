package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.CampoTexto
import com.poligono.cop.composeApp.componentes.DialogoAvisoPrivacidad
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.composeApp.componentes.TituloSeccion
import com.poligono.cop.data.local.PoligonosRespaldo
import com.poligono.cop.domain.model.Configuracion
import com.poligono.cop.plataforma.abrirComoLlegar
import com.poligono.cop.plataforma.abrirWhatsapp
import com.poligono.cop.plataforma.llamarTelefono
import com.poligono.cop.ui.AyudaViewModel

/**
 * COP-RS-01: guía breve.  COP-RS-07: contacto y reporte de fallas.
 * COP-RC-05: aviso de privacidad.
 */
@Composable
fun AyudaScreen(
    configuracion: Configuracion,
    conSesion: Boolean,
    onRegresar: () -> Unit,
    vm: AyudaViewModel = viewModel { AyudaViewModel() }
) {
    val estado by vm.estado.collectAsState()
    var verAviso by remember { mutableStateOf(false) }
    val poligono = PoligonosRespaldo.principal

    PantallaSimple(titulo = "Ayuda y contacto", onRegresar = onRegresar) {
        TituloSeccion("Contacto del Centro")
        Text(poligono.nombre)
        Text(poligono.direccion)

        Button(onClick = { llamarTelefono(configuracion.telefonoCentro) }, modifier = Modifier.fillMaxWidth()) {
            Text("☎️ Llamar: ${configuracion.telefonoCentro}")
        }
        OutlinedButton(
            onClick = { abrirWhatsapp(configuracion.whatsappCentro, "Hola, tengo una duda sobre mi cita en COP.") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💬 WhatsApp")
        }
        OutlinedButton(
            onClick = { abrirComoLlegar(poligono.latitud, poligono.longitud, poligono.nombre) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📍 Cómo llegar")
        }

        TituloSeccion("Preguntas frecuentes")
        Text("¿Cómo agendo? En tu panel toca \"Agendar una cita\": elige servicio, profesional, día y hora, y confirma.")
        Text("¿Cuándo queda confirmada? Cuando el personal del Centro la revise. Te llega un aviso.")
        Text("¿Puedo cancelar o cambiar mi cita? Sí, desde tu panel, con al menos ${configuracion.plazoHorasCambio} horas de anticipación.")
        Text("¿Me recuerdan la cita? Sí, ${configuracion.horasRecordatorio} horas antes recibes un recordatorio.")
        Text("¿Funciona sin internet? Puedes ver tus citas guardadas; para agendar o cambiar necesitas conexión.")
        Text("¿Puedo entrar con huella? Sí: al iniciar sesión marca \"Recordar para entrar con huella\".")

        TextButton(onClick = { verAviso = true }) {
            Text("Leer aviso de privacidad")
        }

        if (conSesion) {
            TituloSeccion("Reportar una falla")
            CampoTexto(estado.descripcion, vm::onDescripcionChange, "¿Qué pasó?", lineas = 4)

            estado.error?.let { Aviso(it, esError = true) }
            estado.info?.let { Aviso(it) }

            Button(
                onClick = vm::enviar,
                enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar reporte")
            }
        }
    }

    if (verAviso) {
        DialogoAvisoPrivacidad(onCerrar = { verAviso = false })
    }
}
