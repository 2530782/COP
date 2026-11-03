package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Horario
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.usecase.Validar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Servicios y profesionales (COP-RN-17 y COP-RE-01). */
data class CatalogoUiState(
    val cargando: Boolean = true,
    val servicios: List<Servicio> = emptyList(),
    val profesionales: List<Profesional> = emptyList(),
    val mensaje: String? = null,
    val error: String? = null
)

class CatalogoViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(CatalogoUiState())
    val estado: StateFlow<CatalogoUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            val servicios = repository.obtenerServicios()
            val profesionales = repository.obtenerProfesionales(todos = true)

            _estado.update {
                it.copy(
                    cargando = false,
                    servicios = servicios.getOrDefault(it.servicios),
                    profesionales = profesionales.getOrDefault(it.profesionales),
                    error = (servicios.exceptionOrNull() ?: profesionales.exceptionOrNull())?.message
                )
            }
        }
    }

    fun guardarServicio(servicio: Servicio, esNuevo: Boolean) {
        if (servicio.id.isBlank() || servicio.nombre.isBlank() || servicio.descripcion.isBlank()) {
            _estado.update { it.copy(error = "Escribe clave, nombre y descripción.") }
            return
        }

        if (servicio.duracionMinutos !in 10..240) {
            _estado.update { it.copy(error = "La duración debe ser de 10 a 240 minutos.") }
            return
        }

        viewModelScope.launch {
            repository.guardarServicio(servicio, esNuevo)
                .onSuccess {
                    _estado.update { it.copy(mensaje = "Servicio guardado.", error = null) }
                    cargar()
                }
                .onFailure { error -> _estado.update { it.copy(error = error.message, mensaje = null) } }
        }
    }

    fun guardarProfesional(
        username: String,
        password: String,
        nombre: String,
        servicioId: String?,
        activo: Boolean,
        anterior: Profesional?
    ) {
        val esNuevo = anterior == null
        val error = Validar.nombre(nombre)
            ?: (if (esNuevo) Validar.username(username) else null)
            ?: (if (esNuevo || password.isNotBlank()) Validar.contrasena(password) else null)
            ?: (if (servicioId == null) "Elige el servicio que atiende." else null)

        if (error != null || servicioId == null) {
            _estado.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            repository.guardarProfesional(username.trim(), password, nombre.trim(), servicioId, activo, esNuevo)
                .onSuccess {
                    val texto = if (anterior != null && anterior.activo && !activo) {
                        "Profesional desactivado. Se avisó a sus pacientes para que reprogramen."
                    } else {
                        "Profesional guardado."
                    }
                    _estado.update { it.copy(mensaje = texto, error = null) }
                    cargar()
                }
                .onFailure { e -> _estado.update { it.copy(error = e.message, mensaje = null) } }
        }
    }

    fun limpiar() {
        _estado.update { it.copy(mensaje = null, error = null) }
    }
}

/** Horarios de atención (COP-RN-14). */
data class HorariosUiState(
    val profesionales: List<Profesional> = emptyList(),
    val profesional: Profesional? = null,
    val horarios: List<Horario> = emptyList(),
    val dia: Int = 1,
    val inicio: String = "09:00",
    val fin: String = "14:00",
    val mensaje: String? = null,
    val error: String? = null
)

class HorariosViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(HorariosUiState())
    val estado: StateFlow<HorariosUiState> = _estado.asStateFlow()

    private val regexHora = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

    init {
        viewModelScope.launch {
            repository.obtenerProfesionales()
                .onSuccess { lista -> _estado.update { it.copy(profesionales = lista) } }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }

    fun elegirProfesional(profesional: Profesional) {
        _estado.update { it.copy(profesional = profesional, horarios = emptyList(), mensaje = null) }
        cargarHorarios()
    }

    fun onDiaChange(dia: Int) = _estado.update { it.copy(dia = dia) }
    fun onInicioChange(v: String) = _estado.update { it.copy(inicio = v.take(5)) }
    fun onFinChange(v: String) = _estado.update { it.copy(fin = v.take(5)) }

    fun agregar() {
        val e = _estado.value
        val p = e.profesional ?: return

        val error = when {
            !regexHora.matches(e.inicio) || !regexHora.matches(e.fin) -> "Escribe las horas como 09:00."
            e.inicio >= e.fin -> "La hora de inicio debe ser antes de la de fin."
            else -> null
        }

        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            repository.agregarHorario(p.username, e.dia, e.inicio, e.fin)
                .onSuccess { lista -> _estado.update { it.copy(horarios = lista, mensaje = "Horario agregado.", error = null) } }
                .onFailure { err -> _estado.update { it.copy(error = err.message, mensaje = null) } }
        }
    }

    fun eliminar(horario: Horario) {
        viewModelScope.launch {
            repository.eliminarHorario(horario.id)
                .onSuccess {
                    _estado.update { it.copy(mensaje = "Horario eliminado. Las citas ya agendadas se conservan.", error = null) }
                    cargarHorarios()
                }
                .onFailure { err -> _estado.update { it.copy(error = err.message) } }
        }
    }

    private fun cargarHorarios() {
        val p = _estado.value.profesional ?: return

        viewModelScope.launch {
            repository.obtenerHorarios(p.username)
                .onSuccess { lista -> _estado.update { it.copy(horarios = lista) } }
                .onFailure { err -> _estado.update { it.copy(error = err.message) } }
        }
    }
}
