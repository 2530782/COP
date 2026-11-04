package com.poligono.cop.domain.ubicacion

/** Una lectura del sensor GPS. */
data class Coordenada(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float = 0f,
    val momento: Long = 0
)
