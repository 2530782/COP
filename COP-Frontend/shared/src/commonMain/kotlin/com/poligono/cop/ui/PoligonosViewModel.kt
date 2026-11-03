package com.poligono.cop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.ubicacion.Coordenada
import com.poligono.cop.domain.usecase.PoligonoConDistancia
import com.poligono.cop.domain.usecase.PoligonosCercanosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla del mapa (sensor GPS). */
data class PoligonosUiState(
    val poligonos: List<Poligono> = emptyList(),
    val sinConexion: Boolean = false,
    val seleccionadoId: Int? = null,
    /** Punto al que se mueve el mapa (cambia al tocar "Ver en mapa"). */
    val enfoque: Coordenada? = null
)

/**
 * Polígonos de Tulancingo: primero los guardados en el teléfono (o la copia de la app)
 * para que el mapa aparezca al instante, después los oficiales del servidor.
 * La ubicación la da el sensor GPS (composeApp/sensores) y el caso de uso calcula distancias.
 */
class PoligonosViewModel(
    private val repository: CopRepository = Dependencias.repository,
    private val cercanos: PoligonosCercanosUseCase = Dependencias.poligonosCercanos
) : ViewModel() {

    private val _estado = MutableStateFlow(PoligonosUiState(poligonos = repository.poligonosGuardados()))
    val estado: StateFlow<PoligonosUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerPoligonos()
                .onSuccess { lista ->
                    if (lista.isNotEmpty()) {
                        _estado.update { it.copy(poligonos = lista, sinConexion = false) }
                    }
                }
                .onFailure { _estado.update { it.copy(sinConexion = true) } }
        }
    }

    /** Procesamiento del GPS: polígonos ordenados del más cercano al más lejano. */
    fun ordenarPorDistancia(ubicacion: Coordenada?): List<PoligonoConDistancia> =
        cercanos(_estado.value.poligonos, ubicacion)

    fun seleccionar(poligono: Poligono) {
        _estado.update { it.copy(seleccionadoId = poligono.id) }
    }

    fun verEnMapa(poligono: Poligono) {
        _estado.update {
            it.copy(
                seleccionadoId = poligono.id,
                enfoque = Coordenada(poligono.latitud, poligono.longitud, momento = poligono.id.toLong())
            )
        }
    }
}
