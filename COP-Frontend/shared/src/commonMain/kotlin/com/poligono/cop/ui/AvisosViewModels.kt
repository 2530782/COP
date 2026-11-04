package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Notificacion
import com.poligono.cop.domain.model.Reporte
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.plataforma.nombreDispositivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Avisos del Centro (COP-RN-11). */
data class NotificacionesUiState(
    val cargando: Boolean = true,
    val avisos: List<Notificacion> = emptyList(),
    val error: String? = null
)

class NotificacionesViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(NotificacionesUiState())
    val estado: StateFlow<NotificacionesUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            repository.obtenerNotificaciones()
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, avisos = lista, error = null) } }
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = error.message) } }
        }
    }

    fun marcarLeida(aviso: Notificacion) {
        if (aviso.leida) return

        viewModelScope.launch {
            repository.marcarNotificacionLeida(aviso.id)
            cargar()
        }
    }

    fun marcarTodas() {
        viewModelScope.launch {
            repository.marcarTodasLeidas()
            cargar()
        }
    }
}

/** Reportar una falla desde Ayuda (COP-RS-07). */
data class AyudaUiState(
    val descripcion: String = "",
    val enviando: Boolean = false,
    val info: String? = null,
    val error: String? = null
)

class AyudaViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(AyudaUiState())
    val estado: StateFlow<AyudaUiState> = _estado.asStateFlow()

    fun onDescripcionChange(texto: String) {
        _estado.update { it.copy(descripcion = texto.take(1000), error = null) }
    }

    fun enviar() {
        val texto = _estado.value.descripcion.trim()

        if (texto.isEmpty()) {
            _estado.update { it.copy(error = "Describe la falla, por favor.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null, info = null) }

            repository.enviarReporte(texto, nombreDispositivo())
                .onSuccess { mensaje -> _estado.update { it.copy(enviando = false, descripcion = "", info = mensaje) } }
                .onFailure { error -> _estado.update { it.copy(enviando = false, error = error.message) } }
        }
    }
}

/** Lista de reportes de fallas para el personal. */
data class ReportesUiState(
    val cargando: Boolean = true,
    val reportes: List<Reporte> = emptyList(),
    val error: String? = null
)

class ReportesViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(ReportesUiState())
    val estado: StateFlow<ReportesUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerReportes()
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, reportes = lista) } }
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = error.message) } }
        }
    }
}
