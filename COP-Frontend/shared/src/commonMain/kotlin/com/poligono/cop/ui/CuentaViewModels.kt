package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.usecase.RegistrarUsuarioUseCase
import com.poligono.cop.domain.usecase.Validar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de registro (COP-RN-01). */
data class RegistroUiState(
    val nombre: String = "",
    val telefono: String = "",
    val correo: String = "",
    val username: String = "",
    val password: String = "",
    val confirmar: String = "",
    val aceptaPrivacidad: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    /** Cuando deja de ser null, la cuenta se creó y la sesión quedó iniciada. */
    val usuarioCreado: Usuario? = null
)

class RegistroViewModel(
    private val registrar: RegistrarUsuarioUseCase = Dependencias.registrarUsuario
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun onNombreChange(v: String) = _estado.update { it.copy(nombre = v, error = null) }
    fun onTelefonoChange(v: String) = _estado.update { it.copy(telefono = v.filter { c -> c.isDigit() }.take(10), error = null) }
    fun onCorreoChange(v: String) = _estado.update { it.copy(correo = v, error = null) }
    fun onUsernameChange(v: String) = _estado.update { it.copy(username = v.trim(), error = null) }
    fun onPasswordChange(v: String) = _estado.update { it.copy(password = v, error = null) }
    fun onConfirmarChange(v: String) = _estado.update { it.copy(confirmar = v, error = null) }
    fun onAceptaChange(v: Boolean) = _estado.update { it.copy(aceptaPrivacidad = v, error = null) }

    /** La navegación ya usó la cuenta creada: limpiamos el formulario para la próxima vez. */
    fun registroConsumido() {
        _estado.value = RegistroUiState()
    }

    fun crearCuenta() {
        val e = _estado.value

        if (e.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            registrar(e.username, e.password, e.confirmar, e.nombre, e.telefono, e.correo, e.aceptaPrivacidad)
                .onSuccess { usuario -> _estado.update { it.copy(cargando = false, usuarioCreado = usuario) } }
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = error.message) } }
        }
    }
}

/** Recuperar contraseña con código de 6 dígitos (COP-RN-02). */
data class RecuperarUiState(
    val username: String = "",
    val codigo: String = "",
    val nueva: String = "",
    val codigoEnviado: Boolean = false,
    val cargando: Boolean = false,
    val info: String? = null,
    val error: String? = null
)

class RecuperarViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(RecuperarUiState())
    val estado: StateFlow<RecuperarUiState> = _estado.asStateFlow()

    fun onUsernameChange(v: String) = _estado.update { it.copy(username = v.trim(), error = null) }
    fun onCodigoChange(v: String) = _estado.update { it.copy(codigo = v.filter { c -> c.isDigit() }.take(6), error = null) }
    fun onNuevaChange(v: String) = _estado.update { it.copy(nueva = v, error = null) }

    fun pedirCodigo() {
        val e = _estado.value

        if (e.username.isBlank()) {
            _estado.update { it.copy(error = "Escribe tu usuario o correo.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            repository.recuperarContrasena(e.username)
                .onSuccess { texto -> _estado.update { it.copy(cargando = false, codigoEnviado = true, info = texto) } }
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = error.message) } }
        }
    }

    fun cambiar() {
        val e = _estado.value
        val error = if (e.codigo.length != 6) "El código tiene 6 dígitos." else Validar.contrasena(e.nueva)

        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            repository.restablecerContrasena(e.username, e.codigo, e.nueva)
                .onSuccess { texto -> _estado.update { it.copy(cargando = false, info = texto) } }
                .onFailure { error2 -> _estado.update { it.copy(cargando = false, error = error2.message) } }
        }
    }
}
