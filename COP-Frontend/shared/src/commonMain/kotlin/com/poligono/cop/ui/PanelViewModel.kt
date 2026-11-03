package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.usecase.ObtenerCitasUseCase
import com.poligono.cop.domain.usecase.ObtenerProfesionalesUseCase
import com.poligono.cop.domain.usecase.ObtenerServiciosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de cualquier panel (vista de rol). */
data class PanelUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val citas: List<Cita> = emptyList(),
    val servicios: List<Servicio> = emptyList(),
    val profesionales: List<Profesional> = emptyList()
)

/**
 * ViewModel que usan las 4 vistas de rol.
 * Carga los datos del backend según el rol del usuario que inició sesión.
 */
class PanelViewModel(
    private val usuario: Usuario,
    private val obtenerCitas: ObtenerCitasUseCase = Dependencias.obtenerCitas,
    private val obtenerServicios: ObtenerServiciosUseCase = Dependencias.obtenerServicios,
    private val obtenerProfesionales: ObtenerProfesionalesUseCase = Dependencias.obtenerProfesionales
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelUiState())
    val estado: StateFlow<PanelUiState> = _estado.asStateFlow()

    // init se ejecuta al crear el ViewModel: cargamos los datos de una vez
    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            // 1) Citas: el caso de uso decide cuáles le tocan a este rol
            val citas = obtenerCitas(usuario)

            // 2) Servicios y profesionales: el profesional no los necesita en su vista
            val necesitaCatalogo = usuario.rol != Rol.PROFESIONAL
            val servicios: Result<List<Servicio>> = if (necesitaCatalogo) obtenerServicios() else Result.success(emptyList())
            val profesionales: Result<List<Profesional>> = if (necesitaCatalogo) obtenerProfesionales() else Result.success(emptyList())

            // Si cualquiera falló, mostramos el primer error
            val error = listOf(citas, servicios, profesionales)
                .firstNotNullOfOrNull { it.exceptionOrNull() }

            _estado.update {
                it.copy(
                    cargando = false,
                    error = error?.message,
                    citas = citas.getOrDefault(emptyList()),
                    servicios = servicios.getOrDefault(emptyList()),
                    profesionales = profesionales.getOrDefault(emptyList())
                )
            }
        }
    }
}
