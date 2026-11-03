package com.poligono.cop.domain.model

/** Servicio que ofrece el Centro (COP-RN-03): fisioterapia, nutrición, psicología... */
data class Servicio(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val duracionMinutos: Int,
    val activo: Boolean
)
