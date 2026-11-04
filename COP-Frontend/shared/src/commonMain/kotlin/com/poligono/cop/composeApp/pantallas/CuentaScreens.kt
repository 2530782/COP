package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.composeApp.componentes.CampoTexto
import com.poligono.cop.composeApp.componentes.DialogoAvisoPrivacidad
import com.poligono.cop.composeApp.componentes.PantallaSimple
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.RecuperarViewModel
import com.poligono.cop.ui.RegistroViewModel

/** COP-RN-01: crear cuenta, con aviso de privacidad (COP-RC-05). */
@Composable
fun RegistroScreen(
    onRegistrado: (Usuario) -> Unit,
    onRegresar: () -> Unit,
    vm: RegistroViewModel = viewModel { RegistroViewModel() }
) {
    val estado by vm.estado.collectAsState()
    var verAviso by remember { mutableStateOf(false) }

    // Al crearse la cuenta la sesión ya quedó iniciada: se va al panel del usuario
    LaunchedEffect(estado.usuarioCreado) {
        estado.usuarioCreado?.let { usuario ->
            vm.registroConsumido()
            onRegistrado(usuario)
        }
    }

    PantallaSimple(titulo = "Crear cuenta", onRegresar = onRegresar) {
        CampoTexto(estado.nombre, vm::onNombreChange, "Nombre completo")
        CampoTexto(estado.telefono, vm::onTelefonoChange, "Teléfono (10 dígitos)", KeyboardType.Phone)
        CampoTexto(estado.correo, vm::onCorreoChange, "Correo (opcional)", KeyboardType.Email)
        CampoTexto(estado.username, vm::onUsernameChange, "Usuario para entrar (sin espacios)")
        CampoTexto(estado.password, vm::onPasswordChange, "Contraseña (6 o más, letras y números)", KeyboardType.Password, esContrasena = true)
        CampoTexto(estado.confirmar, vm::onConfirmarChange, "Repite la contraseña", KeyboardType.Password, esContrasena = true)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = estado.aceptaPrivacidad,
                onCheckedChange = vm::onAceptaChange
            )
            Text("He leído y acepto el aviso de privacidad")
        }

        TextButton(onClick = { verAviso = true }) {
            Text("Leer aviso de privacidad")
        }

        estado.error?.let { Aviso(it, esError = true) }

        Button(
            onClick = vm::crearCuenta,
            enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (estado.cargando) "Creando cuenta…" else "Crear cuenta")
        }
    }

    if (verAviso) {
        DialogoAvisoPrivacidad(onCerrar = { verAviso = false })
    }
}

/** COP-RN-02: recuperar la contraseña con un código de 6 dígitos. */
@Composable
fun RecuperarScreen(
    onRegresar: () -> Unit,
    vm: RecuperarViewModel = viewModel { RecuperarViewModel() }
) {
    val estado by vm.estado.collectAsState()

    PantallaSimple(titulo = "Recuperar contraseña", onRegresar = onRegresar) {
        Text("Escribe tu usuario o tu correo. Te daremos un código de 6 dígitos para crear una contraseña nueva.")

        CampoTexto(estado.username, vm::onUsernameChange, "Usuario o correo")

        if (!estado.codigoEnviado) {
            Button(
                onClick = vm::pedirCodigo,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solicitar código")
            }
        } else {
            CampoTexto(estado.codigo, vm::onCodigoChange, "Código de 6 dígitos", KeyboardType.Number)
            CampoTexto(estado.nueva, vm::onNuevaChange, "Contraseña nueva", KeyboardType.Password, esContrasena = true)

            Button(
                onClick = vm::cambiar,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cambiar contraseña")
            }
        }

        estado.info?.let { Aviso(it) }
        estado.error?.let { Aviso(it, esError = true) }
    }
}
