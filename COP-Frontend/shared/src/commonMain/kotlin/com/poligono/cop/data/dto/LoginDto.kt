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

/** Lo que el backend RESPONDE a POST /login y POST /registro. */
@Serializable
data class LoginResponse(
    val exito: Boolean,
    val mensaje: String,
    val usuario: UsuarioDto? = null,   // solo viene cuando exito = true
    val token: String? = null          // se manda en las demás peticiones (Authorization: Bearer)
)

/** Datos públicos del usuario (sin contraseña). */
@Serializable
data class UsuarioDto(
    val username: String,
    val nombre: String,
    val rol: String,                  // "ADMINISTRADOR", "PERSONAL", "PROFESIONAL" o "USUARIO"
    val servicioId: String? = null,
    val telefono: String = "",
    val correo: String = ""
)

/** POST /registro (COP-RN-01). */
@Serializable
data class RegistroRequest(
    val username: String,
    val password: String,
    val nombre: String,
    val telefono: String,
    val correo: String = "",
    val aceptaPrivacidad: Boolean
)

/** POST /recuperar y su respuesta (COP-RN-02). */
@Serializable
data class RecuperarRequest(
    val username: String
)

@Serializable
data class RecuperarResponse(
    val exito: Boolean,
    val mensaje: String,
    val codigoDemo: String? = null
)

/** POST /restablecer. */
@Serializable
data class RestablecerRequest(
    val username: String,
    val codigo: String,
    val nuevaPassword: String
)

/** Respuesta del backend cuando algo sale mal (400, 401, 403, 404, 409...). */
@Serializable
data class RespuestaError(
    val exito: Boolean = false,
    val mensaje: String
)

/** Respuesta del backend cuando todo sale bien y no hay más datos. */
@Serializable
data class RespuestaOk(
    val exito: Boolean = true,
    val mensaje: String
)
