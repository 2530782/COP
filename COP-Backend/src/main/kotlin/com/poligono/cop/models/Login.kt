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

/**
 * JSON que se responde en POST /login.
 * token: se manda en las siguientes peticiones como "Authorization: Bearer <token>".
 */
@Serializable
data class LoginResponse(
    val exito: Boolean,
    val mensaje: String,
    val usuario: UsuarioDto? = null,
    val token: String? = null
)

/** Respuesta genérica de error (mismo formato que LoginResponse, sin usuario). */
@Serializable
data class RespuestaError(
    val exito: Boolean,
    val mensaje: String
)

/** Respuesta genérica de éxito sin datos. */
@Serializable
data class RespuestaOk(
    val exito: Boolean = true,
    val mensaje: String
)
