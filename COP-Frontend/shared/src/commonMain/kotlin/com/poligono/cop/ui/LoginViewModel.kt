package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.usecase.IniciarSesionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Todo lo que la pantalla de Login necesita saber para dibujarse.
 * Es inmutable (val): para cambiar algo se crea una copia con .copy(...)
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    /** Cuando deja de ser null, el login fue exitoso y hay que navegar. */
    val usuarioAutenticado: Usuario? = null,
    /** El usuario marcó "Recordar para entrar con huella". */
    val recordarHuella: Boolean = false
)

/**
 * ViewModel del Login.
 * - Guarda el estado de la pantalla (sobrevive a girar el teléfono).
 * - Recibe los eventos de la pantalla (escribir, presionar "Entrar").
 * - Llama al caso de uso IniciarSesionUseCase (capa domain).
 */
class LoginViewModel(
    private val iniciarSesion: IniciarSesionUseCase = Dependencias.iniciarSesion
) : ViewModel() {

    // _estado es privado y MUTABLE: solo el ViewModel lo cambia.
    private val _estado = MutableStateFlow(LoginUiState())

    // estado es público y de SOLO LECTURA: la pantalla lo observa.
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onUsernameChange(nuevo: String) {
        _estado.update { it.copy(username = nuevo, error = null) }
    }

    fun onPasswordChange(nuevo: String) {
        _estado.update { it.copy(password = nuevo, error = null) }
    }

    fun onRecordarChange(valor: Boolean) {
        _estado.update { it.copy(recordarHuella = valor) }
    }

    /** Muestra un mensaje de error en la pantalla (ej. fallo del lector de huella). */
    fun mostrarError(mensaje: String) {
        _estado.update { it.copy(error = mensaje) }
    }

    fun entrar() {
        val actual = _estado.value
        if (actual.cargando) return   // evita doble clic

        // viewModelScope: corrutina que se cancela sola si el ViewModel se destruye
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            iniciarSesion(actual.username, actual.password)
                .onSuccess { usuario ->
                    _estado.update {
                        it.copy(cargando = false, usuarioAutenticado = usuario)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(cargando = false, error = error.message ?: "Ocurrió un error inesperado")
                    }
                }
        }
    }

    /** La navegación ya usó al usuario autenticado: limpiamos para la próxima vez. */
    fun sesionConsumida() {
        _estado.value = LoginUiState()
    }
}
