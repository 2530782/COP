package com.poligono.cop.models

import kotlinx.serialization.Serializable

/*
 * Modelos que viajan por la red. DEBEN tener los mismos nombres de campos
 * que los DTOs del frontend (COP-Frontend/shared/.../data/dto/LoginDto.kt).
 */

/** JSON que llega en POST /login  ->  {"username":"ana","password":"ana123"} */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/** JSON que se responde en POST /login. */
@Serializable
data class LoginResponse(
    val exito: Boolean,
    val mensaje: String,
    val usuario: UsuarioDto? = null
)

/** Respuesta genérica de error (mismo formato que LoginResponse, sin usuario). */
@Serializable
data class RespuestaError(
    val exito: Boolean,
    val mensaje: String
)
