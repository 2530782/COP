package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada
import com.poligono.cop.domain.ubicacion.Distancias

/** Un polígono con la distancia a la persona (null si todavía no hay GPS). */
data class PoligonoConDistancia(
    val poligono: Poligono,
    val metros: Double?
)

/**
 * Caso de uso del sensor GPS (procesamiento):
 * calcula la distancia de la persona a cada polígono y los ordena del más cercano al más lejano.
 */
class PoligonosCercanosUseCase {

    operator fun invoke(
        poligonos: List<Poligono>,
        ubicacion: Coordenada?
    ): List<PoligonoConDistancia> {
        return poligonos
            .map { p ->
                val metros = ubicacion?.let {
                    Distancias.metros(it.latitud, it.longitud, p.latitud, p.longitud)
                }
                PoligonoConDistancia(p, metros)
            }
            .sortedBy { it.metros ?: Double.MAX_VALUE }
    }
}
