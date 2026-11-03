package com.poligono.cop.data

import com.poligono.cop.models.ProfesionalDto
import com.poligono.cop.models.UsuarioDto
import java.sql.ResultSet

/** Fila completa de la tabla usuarios (con datos que NUNCA se mandan a la app). */
data class UsuarioFila(
    val username: String,
    val contrasenaHash: String,
    val nombre: String,
    val rol: String,
    val servicioId: String?,
    val telefono: String,
    val correo: String,
    val activo: Boolean,
    val intentosFallidos: Int,
    val bloqueadoHasta: Long,
    val codigoRecuperacion: String?,
    val codigoExpira: Long
) {
    /** Versión pública del usuario: SIN contraseña. */
    fun aPublico(): UsuarioDto = UsuarioDto(username, nombre, rol, servicioId, telefono, correo)
}

/** Consultas a la tabla usuarios (incluye a los profesionales). */
object UsuarioRepositorio {

    private fun mapear(rs: ResultSet): UsuarioFila {
        return UsuarioFila(
            username = rs.getString("username"),
            contrasenaHash = rs.getString("contrasena_hash"),
            nombre = rs.getString("nombre"),
            rol = rs.getString("rol"),
            servicioId = rs.getString("servicio_id"),
            telefono = rs.getString("telefono") ?: "",
            correo = rs.getString("correo") ?: "",
            activo = rs.getBoolean("activo"),
            intentosFallidos = rs.getInt("intentos_fallidos"),
            bloqueadoHasta = rs.getLong("bloqueado_hasta"),
            codigoRecuperacion = rs.getString("codigo_recuperacion"),
            codigoExpira = rs.getLong("codigo_expira")
        )
    }

    /** Busca por username o por correo (los dos sirven para entrar). */
    fun buscar(usuarioOCorreo: String): UsuarioFila? {
        val texto = usuarioOCorreo.trim().lowercase()

        return BaseDatos.consultar(
            "SELECT * FROM usuarios WHERE username = ? OR (correo <> '' AND LOWER(correo) = ?)",
            texto,
            texto
        ) { mapear(it) }.firstOrNull()
    }

    fun existeCorreo(correo: String): Boolean {
        return BaseDatos.entero(
            "SELECT COUNT(*) FROM usuarios WHERE correo <> '' AND LOWER(correo) = ?",
            correo.trim().lowercase()
        ) > 0
    }

    fun crear(
        username: String,
        contrasenaHash: String,
        nombre: String,
        rol: String,
        servicioId: String?,
        telefono: String,
        correo: String,
        aceptoPrivacidad: Boolean
    ) {
        BaseDatos.ejecutar(
            """
            INSERT INTO usuarios
                (username, contrasena_hash, nombre, rol, servicio_id, telefono, correo, activo, acepto_privacidad, creado)
            VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)
            """.trimIndent(),
            username.trim().lowercase(),
            contrasenaHash,
            nombre.trim(),
            rol,
            servicioId,
            telefono.trim(),
            correo.trim().lowercase(),
            aceptoPrivacidad,
            System.currentTimeMillis()
        )
    }

    fun guardarIntentos(
        username: String,
        intentos: Int,
        bloqueadoHasta: Long
    ) {
        BaseDatos.ejecutar(
            "UPDATE usuarios SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE username = ?",
            intentos,
            bloqueadoHasta,
            username
        )
    }

    fun guardarCodigo(
        username: String,
        codigo: String?,
        expira: Long
    ) {
        BaseDatos.ejecutar(
            "UPDATE usuarios SET codigo_recuperacion = ?, codigo_expira = ? WHERE username = ?",
            codigo,
            expira,
            username
        )
    }

    fun cambiarContrasena(
        username: String,
        contrasenaHash: String
    ) {
        BaseDatos.ejecutar(
            """
            UPDATE usuarios
            SET contrasena_hash = ?, codigo_recuperacion = NULL, codigo_expira = 0,
                intentos_fallidos = 0, bloqueado_hasta = 0
            WHERE username = ?
            """.trimIndent(),
            contrasenaHash,
            username
        )
    }

    // ---------- Profesionales (usuarios con rol PROFESIONAL) ----------

    fun profesionales(
        servicioId: String?,
        soloActivos: Boolean
    ): List<ProfesionalDto> {
        val condiciones = mutableListOf("rol = 'PROFESIONAL'")
        val parametros = mutableListOf<Any?>()

        if (servicioId != null) {
            condiciones.add("servicio_id = ?")
            parametros.add(servicioId)
        }

        if (soloActivos) {
            condiciones.add("activo = TRUE")
        }

        return BaseDatos.consultar(
            "SELECT * FROM usuarios WHERE ${condiciones.joinToString(" AND ")} ORDER BY nombre",
            *parametros.toTypedArray()
        ) { rs ->
            ProfesionalDto(
                username = rs.getString("username"),
                nombre = rs.getString("nombre"),
                servicioId = rs.getString("servicio_id"),
                activo = rs.getBoolean("activo")
            )
        }
    }

    fun actualizarProfesional(
        username: String,
        nombre: String,
        servicioId: String,
        activo: Boolean
    ) {
        BaseDatos.ejecutar(
            "UPDATE usuarios SET nombre = ?, servicio_id = ?, activo = ? WHERE username = ? AND rol = 'PROFESIONAL'",
            nombre.trim(),
            servicioId,
            activo,
            username
        )
    }
}
