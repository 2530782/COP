package com.poligono.cop.services

import io.ktor.http.HttpStatusCode

/**
 * Error con un mensaje claro para mostrarlo tal cual en la app (COP-RNN-09).
 * HTTP.kt lo convierte en  {"exito": false, "mensaje": "..."}  con su código HTTP.
 */
class ErrorNegocio(
    val estado: HttpStatusCode,
    mensaje: String
) : Exception(mensaje)
