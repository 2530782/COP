package com.poligono.cop.models

import kotlinx.serialization.Serializable

/** Roles de COP. El texto es EXACTAMENTE el que viaja en el JSON. */
object Roles {
    const val ADMINISTRADOR = "ADMINISTRADOR"
    const val PERSONAL = "PERSONAL"
    const val PROFESIONAL = "PROFESIONAL"
    const val USUARIO = "USUARIO"

    /** Quienes administran la agenda del Centro. */
    val PERSONAL_DEL_CENTRO = arrayOf(ADMINISTRADOR, PERSONAL)
}

/**
 * Así viene CADA usuario dentro de resources/data/usuarios.json.
 * Ahora solo se usa como DATOS INICIALES: al arrancar por primera vez se copian
 * a la base de datos con la contraseña CIFRADA (ver data/Esquema.kt).
 */
@Serializable
data class UsuarioRegistro(
    val username: String,
    val password: String,
    val nombre: String,
    val rol: String,                 // ADMINISTRADOR | PERSONAL | PROFESIONAL | USUARIO
    val servicioId: String? = null,  // solo para PROFESIONAL
    val activo: Boolean = true,
    val telefono: String = "",
    val correo: String = ""
)

/** Lo que el frontend sí puede ver de un usuario (SIN contraseña). */
@Serializable
data class UsuarioDto(
    val username: String,
    val nombre: String,
    val rol: String,
    val servicioId: String? = null,
    val telefono: String = "",
    val correo: String = ""
)

/** Respuesta de GET /profesionales. */
@Serializable
data class ProfesionalDto(
    val username: String,
    val nombre: String,
    val servicioId: String? = null,
    val activo: Boolean = true
)

/** POST /registro: una persona crea su cuenta (COP-RN-01). */
@Serializable
data class RegistroRequest(
    val username: String,
    val password: String,
    val nombre: String,
    val telefono: String,
    val correo: String = "",
    val aceptaPrivacidad: Boolean
)

/** POST /profesionales y PUT /profesionales/{username} (COP-RN-17). */
@Serializable
data class ProfesionalRequest(
    val username: String,
    val password: String = "",       // solo al crear; vacío = no se cambia
    val nombre: String,
    val servicioId: String,
    val activo: Boolean = true
)

/** POST /recuperar: pide un código para cambiar la contraseña (COP-RN-02). */
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

/** POST /restablecer: cambia la contraseña con el código. */
@Serializable
data class RestablecerRequest(
    val username: String,
    val codigo: String,
    val nuevaPassword: String
)
