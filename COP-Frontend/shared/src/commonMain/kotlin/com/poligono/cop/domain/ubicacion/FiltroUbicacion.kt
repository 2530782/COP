package com.poligono.cop.domain.ubicacion

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calibración y procesamiento de las lecturas del GPS.
 * - Descarta lecturas poco precisas (por ejemplo, dentro de un edificio).
 * - Promedia las últimas lecturas dando más peso a las más precisas,
 *   para que el punto azul no "brinque" en el mapa.
 */
class FiltroUbicacion(
    private val precisionMaximaMetros: Float = 100f,
    private val tamanoVentana: Int = 3
) {

    private val ultimas = ArrayDeque<Coordenada>()

    /** Devuelve la ubicación procesada, o null si la lectura se descartó. */
    fun procesar(nueva: Coordenada): Coordenada? {
        if (nueva.precisionMetros > precisionMaximaMetros) {
            return null
        }

        ultimas.addLast(nueva)

        while (ultimas.size > tamanoVentana) {
            ultimas.removeFirst()
        }

        var pesoTotal = 0.0
        var latitud = 0.0
        var longitud = 0.0

        for (lectura in ultimas) {
            val peso = 1.0 / max(lectura.precisionMetros.toDouble(), 1.0)
            pesoTotal += peso
            latitud += lectura.latitud * peso
            longitud += lectura.longitud * peso
        }

        return Coordenada(
            latitud = latitud / pesoTotal,
            longitud = longitud / pesoTotal,
            precisionMetros = nueva.precisionMetros,
            momento = nueva.momento
        )
    }

    fun reiniciar() {
        ultimas.clear()
    }
}

object Distancias {

    private const val RADIO_TIERRA_M = 6_371_000.0

    /** Distancia en metros entre dos puntos (fórmula de Haversine). */
    fun metros(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = radianes(lat2 - lat1)
        val dLon = radianes(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(radianes(lat1)) * cos(radianes(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return RADIO_TIERRA_M * c
    }

    /** Ejemplo: "850 m" o "3.4 km". */
    fun texto(metros: Double): String {
        if (metros < 1000) {
            val redondeado = (metros / 10).roundToInt() * 10
            return "$redondeado m"
        }

        val decimas = (metros / 100).roundToInt()
        return "${decimas / 10}.${decimas % 10} km"
    }

    /** Tiempo aproximado caminando a 4.5 km/h. */
    fun minutosCaminando(metros: Double): Int {
        return max(1, (metros / 75.0).roundToInt())
    }

    private fun radianes(grados: Double): Double {
        return grados * kotlin.math.PI / 180.0
    }
}
