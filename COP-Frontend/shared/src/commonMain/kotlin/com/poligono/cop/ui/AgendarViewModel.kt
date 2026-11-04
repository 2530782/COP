package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.FechaDisponible
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.usecase.AgendarCitaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del asistente para agendar en 5 pasos (COP-RC-06). */
data class AgendarUiState(
    val paso: Int = 1,
    val servicios: List<Servicio> = emptyList(),
    val profesionales: List<Profesional> = emptyList(),
    val fechas: List<FechaDisponible> = emptyList(),
    val horas: List<String> = emptyList(),
    val servicio: Servicio? = null,
    val profesional: Profesional? = null,
    val fecha: String? = null,
    val hora: String? = null,
    val notas: String = "",
    // Solo para la cita presencial o telefónica (COP-RN-15)
    val nombreContacto: String = "",
    val telefonoContacto: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    /** Cuando deja de ser null, la cita ya se registró. */
    val citaCreada: Cita? = null
)

/**
 * ViewModel para agendar:
 *  - el USUARIO solicita su cita (COP-RN-03, 04 y 05)
 *  - el PERSONAL registra una cita presencial o telefónica ([manual] = true, COP-RN-15)
 * Pasos: 1 servicio, 2 profesional, 3 día, 4 hora, 5 confirmar.
 */
class AgendarViewModel(
    private val manual: Boolean,
    servicioInicial: String?,
    private val agendar: AgendarCitaUseCase = Dependencias.agendarCita,
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(AgendarUiState())
    val estado: StateFlow<AgendarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }

            repository.obtenerServicios()
                .onSuccess { lista ->
                    val activos = lista.filter { it.activo }
                    _estado.update { it.copy(cargando = false, servicios = activos) }

                    activos.firstOrNull { it.id == servicioInicial }?.let { elegirServicio(it) }
                }
                .onFailure { error ->
                    _estado.update { it.copy(cargando = false, error = error.message) }
                }
        }
    }

    fun elegirServicio(servicio: Servicio) {
        _estado.update {
            it.copy(paso = 2, servicio = servicio, profesional = null, fecha = null, hora = null, error = null)
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val resultado = repository.obtenerProfesionales(servicioId = servicio.id)
            _estado.update {
                it.copy(
                    cargando = false,
                    profesionales = resultado.getOrDefault(emptyList()),
                    error = resultado.exceptionOrNull()?.message
                )
            }
        }
    }

    fun elegirProfesional(profesional: Profesional) {
        _estado.update { it.copy(paso = 3, profesional = profesional, fecha = null, hora = null, error = null) }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, fechas = emptyList()) }
            val resultado = repository.fechasDisponibles(profesional.username)
            _estado.update {
                it.copy(
                    cargando = false,
                    fechas = resultado.getOrDefault(emptyList()),
                    error = resultado.exceptionOrNull()?.message
                )
            }
        }
    }

    fun elegirFecha(fecha: String) {
        val profesional = _estado.value.profesional ?: return
        _estado.update { it.copy(paso = 4, fecha = fecha, hora = null, error = null, horas = emptyList()) }
        cargarHoras(profesional.username, fecha)
    }

    fun elegirHora(hora: String) {
        _estado.update { it.copy(paso = 5, hora = hora, error = null) }
    }

    fun onNotasChange(texto: String) {
        _estado.update { it.copy(notas = texto.take(500)) }
    }

    fun onNombreChange(texto: String) {
        _estado.update { it.copy(nombreContacto = texto) }
    }

    fun onTelefonoChange(texto: String) {
        _estado.update { it.copy(telefonoContacto = texto.filter { c -> c.isDigit() }.take(10)) }
    }

    /** Botón "atrás" dentro del asistente. Regresa false si ya está en el primer paso. */
    fun pasoAnterior(): Boolean {
        val actual = _estado.value

        if (actual.paso <= 1 || actual.citaCreada != null) {
            return false
        }

        _estado.update { it.copy(paso = it.paso - 1, error = null) }
        return true
    }

    /** Para registrar otra cita manual sin salir de la pantalla. */
    fun otraCita() {
        _estado.update {
            it.copy(paso = 1, citaCreada = null, hora = null, fecha = null, profesional = null, notas = "", nombreContacto = "", telefonoContacto = "")
        }
    }

    fun confirmar() {
        val e = _estado.value

        if (e.cargando) return   // evita doble clic

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            val resultado = if (manual) {
                agendar.manual(e.profesional?.username, e.fecha, e.hora, e.nombreContacto, e.telefonoContacto, e.notas)
            } else {
                agendar(e.profesional?.username, e.fecha, e.hora, e.notas)
            }

            resultado
                .onSuccess { cita ->
                    _estado.update { it.copy(cargando = false, citaCreada = cita) }
                }
                .onFailure { error ->
                    // Si el horario se ocupó mientras tanto, se regresa a elegir la hora
                    val volver = error.message?.contains("ya no está disponible") == true
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message,
                            paso = if (volver) 4 else it.paso,
                            hora = if (volver) null else it.hora
                        )
                    }

                    val p = e.profesional
                    val f = e.fecha

                    if (volver && p != null && f != null) {
                        cargarHoras(p.username, f)
                    }
                }
        }
    }

    private fun cargarHoras(profesional: String, fecha: String) {
        viewModelScope.launch {
            val resultado = repository.horasLibres(profesional, fecha)
            _estado.update {
                it.copy(
                    horas = resultado.getOrDefault(emptyList()),
                    error = resultado.exceptionOrNull()?.message ?: it.error
                )
            }
        }
    }
}

/** Estado de la pantalla para reprogramar o modificar una cita. */
data class ReprogramarUiState(
    val fechas: List<FechaDisponible> = emptyList(),
    val horas: List<String> = emptyList(),
    val fecha: String? = null,
    val hora: String? = null,
    val motivo: String = "",
    val cargando: Boolean = true,
    val error: String? = null,
    val listo: Boolean = false
)

/**
 * COP-RN-09: el usuario reprograma su cita (vuelve a quedar pendiente).
 * COP-RN-16: el personal la modifica (queda confirmada y se avisa al usuario).
 */
class ReprogramarViewModel(
    private val cita: Cita,
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val _estado = MutableStateFlow(ReprogramarUiState())
    val estado: StateFlow<ReprogramarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val resultado = repository.fechasDisponibles(cita.profesionalUsername)
            _estado.update {
                it.copy(
                    cargando = false,
                    fechas = resultado.getOrDefault(emptyList()),
                    error = resultado.exceptionOrNull()?.message
                )
            }
        }
    }

    fun elegirFecha(fecha: String) {
        _estado.update { it.copy(fecha = fecha, hora = null, horas = emptyList()) }

        viewModelScope.launch {
            // excluirCitaId: el horario actual de esta misma cita también cuenta como libre
            val resultado = repository.horasLibres(cita.profesionalUsername, fecha, cita.id)
            _estado.update {
                it.copy(horas = resultado.getOrDefault(emptyList()), error = resultado.exceptionOrNull()?.message)
            }
        }
    }

    fun elegirHora(hora: String) {
        _estado.update { it.copy(hora = hora, error = null) }
    }

    fun onMotivoChange(texto: String) {
        _estado.update { it.copy(motivo = texto.take(300)) }
    }

    fun guardar() {
        val e = _estado.value
        val fecha = e.fecha
        val hora = e.hora

        if (fecha == null || hora == null) {
            _estado.update { it.copy(error = "Elige el nuevo día y la hora.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            repository.reprogramarCita(cita.id, fecha, hora, e.motivo.trim())
                .onSuccess { _estado.update { it.copy(cargando = false, listo = true) } }
                .onFailure { error -> _estado.update { it.copy(cargando = false, error = error.message) } }
        }
    }
}
