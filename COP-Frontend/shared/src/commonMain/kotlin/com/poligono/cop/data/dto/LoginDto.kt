package com.poligono.cop.data.dto

import kotlinx.serialization.Serializable

/*
 * DTO = Data Transfer Object: clases que representan EXACTAMENTE el JSON
 * que viaja por la red entre el frontend y el backend.
 *
 * @Serializable le pide al plugin kotlinx.serialization que genere el código
 * para convertir la clase a JSON (serializar) y de JSON a clase (deserializar).
 *
 * IMPORTANTE: los nombres de las propiedades deben ser IGUALES a los del backend
 * (COP-Backend/src/main/kotlin/com/poligono/cop/models), si no, el JSON no "embona".
 */

/** Lo que el frontend MANDA al hacer POST /login  ->  {"username":"ana","password":"ana123"} */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/** Lo que el backend RESPONDE a POST /login. */
@Serializable
data class LoginResponse(
    val exito: Boolean,
    val mensaje: String,
    val usuario: UsuarioDto? = null   // solo viene cuando exito = true
)

/** Datos públicos del usuario (sin contraseña). */
@Serializable
data class UsuarioDto(
    val username: String,
    val nombre: String,
    val rol: String,                  // "ADMINISTRADOR", "PERSONAL", "PROFESIONAL" o "USUARIO"
    val servicioId: String? = null
)
