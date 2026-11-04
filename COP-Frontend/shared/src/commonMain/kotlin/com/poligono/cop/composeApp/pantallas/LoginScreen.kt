package com.poligono.cop.composeApp.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.componentes.Aviso
import com.poligono.cop.domain.biometria.AccesoBiometrico
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.LoginViewModel

/** Cuentas de prueba que existen en usuarios.json del backend (para la demostración). */
private val cuentasDePrueba = listOf(
    "admin" to "Administrador",
    "personal" to "Personal encargado",
    "fisio" to "Profesional (Fisioterapia)",
    "ana" to "Usuario (paciente)"
)

/**
 * Pantalla de inicio de sesión.
 * - Lee el estado del LoginViewModel y lo dibuja.
 * - Cuando el login es exitoso, avisa a la navegación con onLoginExitoso(usuario).
 */
@Composable
fun LoginScreen(
    acceso: AccesoBiometrico,
    onLoginExitoso: (Usuario) -> Unit,
    mensajeInicial: String? = null,
    onRegistrarse: () -> Unit = {},
    onRecuperar: () -> Unit = {},
    onVerPoligonos: () -> Unit = {},
    onAyuda: () -> Unit = {},
    viewModel: LoginViewModel = viewModel { LoginViewModel() }
) {
    // collectAsState: cada vez que el StateFlow cambia, la pantalla se vuelve a dibujar
    val estado by viewModel.estado.collectAsState()

    // Estado SOLO visual (no le importa al ViewModel): mostrar u ocultar contraseña
    var verPassword by remember { mutableStateOf(false) }

    // Efecto: cuando aparece un usuario autenticado, navegamos UNA vez
    LaunchedEffect(estado.usuarioAutenticado) {
        estado.usuarioAutenticado?.let { usuario ->
            // Si marcó "Recordar", guardamos las credenciales para la próxima vez con huella
            if (estado.recordarHuella) {
                acceso.guardarCredenciales(estado.username, estado.password)
            }
            viewModel.sesionConsumida()   // limpiamos el formulario para la próxima vez
            onLoginExitoso(usuario)       // y la navegación lo manda a la vista de su rol
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { espacioInterno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacioInterno)
                .verticalScroll(rememberScrollState()),   // por si el teclado tapa los campos
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)        // en tablets no se estira de más (COP-RI-05)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---------- Logo y nombre ----------
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COP",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Citas Oficiales Polígono",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Centro de Desarrollo Comunitario Polígono Guadalupe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                // Ej. "Tu sesión se cerró por inactividad" (COP-RNN-08)
                if (mensajeInicial != null) {
                    Aviso(mensajeInicial)
                    Spacer(Modifier.height(12.dp))
                }

                // ---------- Formulario ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Iniciar sesión",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = estado.username,
                            onValueChange = viewModel::onUsernameChange,
                            label = { Text("Usuario") },
                            singleLine = true,
                            enabled = !estado.cargando,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = estado.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Contraseña") },
                            singleLine = true,
                            enabled = !estado.cargando,
                            visualTransformation = if (verPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()   // muestra ••••
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { viewModel.entrar() }),
                            trailingIcon = {
                                TextButton(onClick = { verPassword = !verPassword }) {
                                    Text(if (verPassword) "Ocultar" else "Ver")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mensaje de error (validación, contraseña incorrecta, cuenta bloqueada, sin conexión)
                        estado.error?.let { mensaje ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(12.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.entrar() },
                            enabled = !estado.cargando,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (estado.cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Entrar", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        // ---------- Huella dactilar ----------
                        // Se vuelve a revisar al tocar "Revisar de nuevo" (por si la persona
                        // acaba de registrar su huella en los ajustes del teléfono).
                        var revision by remember { mutableStateOf(0) }
                        val huellaDisponible = remember(revision) { acceso.disponible() }

                        Spacer(Modifier.height(12.dp))

                        if (!huellaDisponible) {
                            // El teléfono no tiene huella registrada (o no tiene sensor):
                            // se explica en lugar de ocultar la opción.
                            Text(
                                text = "Entrar con huella: registra una huella en " +
                                    "Ajustes > Seguridad de tu teléfono para usar esta opción.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(
                                onClick = { revision++ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Revisar de nuevo")
                            }
                        } else {
                            if (acceso.hayCredenciales()) {
                                OutlinedButton(
                                    onClick = {
                                        acceso.autenticar(
                                            onExito = {
                                                acceso.leerCredenciales()?.let { (usuario, clave) ->
                                                    viewModel.onUsernameChange(usuario)
                                                    viewModel.onPasswordChange(clave)
                                                    viewModel.entrar()
                                                }
                                            },
                                            onError = { viewModel.mostrarError(it) }
                                        )
                                    },
                                    enabled = !estado.cargando,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Entrar con huella", style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = estado.recordarHuella,
                                        onCheckedChange = viewModel::onRecordarChange
                                    )
                                    Text("Recordar para entrar con huella")
                                }
                            }
                        }
                    }
                }

                // ---------- Cuenta nueva, recuperar contraseña, mapa y ayuda ----------
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onRegistrarse,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear una cuenta")
                }
                TextButton(onClick = onRecuperar) {
                    Text("¿Olvidaste tu contraseña?")
                }
                OutlinedButton(
                    onClick = onVerPoligonos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📍 Ver polígonos cercanos (GPS)")
                }
                TextButton(onClick = onAyuda) {
                    Text("Ayuda y contacto")
                }

                Spacer(Modifier.height(24.dp))

                // ---------- Ayuda para la demostración ----------
                Text(
                    text = "Cuentas de prueba (toca una para usarla)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                cuentasDePrueba.forEach { (usuario, rol) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = !estado.cargando) {
                                viewModel.onUsernameChange(usuario)
                                viewModel.onPasswordChange("")
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(usuario, fontWeight = FontWeight.SemiBold)
                        Text(rol, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "La contraseña de cada cuenta es el usuario + 123 (ej. ana123).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
