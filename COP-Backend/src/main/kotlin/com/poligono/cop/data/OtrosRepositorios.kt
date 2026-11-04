package com.poligono.cop.data

import com.poligono.cop.models.NotificacionDto
import com.poligono.cop.models.PoligonoDto
import com.poligono.cop.models.PoligonoRequest
import com.poligono.cop.models.ReporteDto
import org.slf4j.LoggerFactory
import java.sql.ResultSet

/** COP-RN-11: avisos al usuario cuando el Centro confirma, modifica o cancela su cita. */
object NotificacionRepositorio {

    private fun mapear(rs: ResultSet): NotificacionDto {
        return NotificacionDto(
            id = rs.getInt("id"),
            citaId = rs.enteroONulo("cita_id"),
            titulo = rs.getString("titulo"),
            mensaje = rs.getString("mensaje"),
            leida = rs.getBoolean("leida"),
            creado = rs.getLong("creado")
        )
    }

    fun crear(
        username: String,
        citaId: Int?,
        titulo: String,
        mensaje: String
    ) {
        BaseDatos.insertar(
            "INSERT INTO notificaciones (username, cita_id, titulo, mensaje, leida, creado) VALUES (?, ?, ?, ?, FALSE, ?)",
            username,
            citaId,
            titulo,
            mensaje,
            System.currentTimeMillis()
        )
    }

    fun delUsuario(username: String): List<NotificacionDto> {
        return BaseDatos.consultar(
            "SELECT * FROM notificaciones WHERE username = ? ORDER BY creado DESC LIMIT 100",
            username
        ) { mapear(it) }
    }

    fun marcarLeida(
        id: Int,
        username: String
    ) {
        BaseDatos.ejecutar(
            "UPDATE notificaciones SET leida = TRUE WHERE id = ? AND username = ?",
            id,
            username
        )
    }

    fun marcarTodasLeidas(username: String) {
        BaseDatos.ejecutar(
            "UPDATE notificaciones SET leida = TRUE WHERE username = ?",
            username
        )
    }
}

/** Ubicación de los polígonos (Centros de Desarrollo Comunitario) para el mapa con GPS. */
object PoligonoRepositorio {

    private fun mapear(rs: ResultSet): PoligonoDto {
        return PoligonoDto(
            id = rs.getInt("id"),
            nombre = rs.getString("nombre"),
            direccion = rs.getString("direccion"),
            latitud = rs.getDouble("latitud"),
            longitud = rs.getDouble("longitud"),
            horario = rs.getString("horario"),
            telefono = rs.getString("telefono"),
            servicios = rs.getString("servicios"),
            esPrincipal = rs.getBoolean("es_principal"),
            ubicacionAproximada = rs.getBoolean("aproximada")
        )
    }

    fun listar(): List<PoligonoDto> {
        return BaseDatos.consultar(
            "SELECT * FROM poligonos WHERE activo = TRUE ORDER BY es_principal DESC, nombre"
        ) { mapear(it) }
    }

    fun crear(p: PoligonoRequest): Int {
        return BaseDatos.insertar(
            """
            INSERT INTO poligonos
                (nombre, direccion, latitud, longitud, horario, telefono, servicios, es_principal, aproximada, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            """.trimIndent(),
            p.nombre.trim(),
            p.direccion.trim(),
            p.latitud,
            p.longitud,
            p.horario.trim(),
            p.telefono.trim(),
            p.servicios.trim(),
            p.esPrincipal,
            p.ubicacionAproximada
        )
    }

    fun actualizar(
        id: Int,
        p: PoligonoRequest
    ) {
        BaseDatos.ejecutar(
            """
            UPDATE poligonos
            SET nombre = ?, direccion = ?, latitud = ?, longitud = ?, horario = ?,
                telefono = ?, servicios = ?, es_principal = ?, aproximada = ?
            WHERE id = ?
            """.trimIndent(),
            p.nombre.trim(),
            p.direccion.trim(),
            p.latitud,
            p.longitud,
            p.horario.trim(),
            p.telefono.trim(),
            p.servicios.trim(),
            p.esPrincipal,
            p.ubicacionAproximada,
            id
        )
    }
}

/** COP-RS-07: reportes de fallas enviados desde la app. */
object ReporteRepositorio {

    fun crear(
        username: String,
        descripcion: String,
        dispositivo: String
    ) {
        BaseDatos.insertar(
            "INSERT INTO reportes (username, descripcion, dispositivo, creado) VALUES (?, ?, ?, ?)",
            username,
            descripcion.trim(),
            dispositivo.trim(),
            System.currentTimeMillis()
        )
    }

    fun listar(): List<ReporteDto> {
        return BaseDatos.consultar(
            """
            SELECT r.id, r.descripcion, r.dispositivo, r.creado, r.username, u.nombre AS usuario_nombre
            FROM reportes r
            LEFT JOIN usuarios u ON u.username = r.username
            ORDER BY r.creado DESC
            LIMIT 200
            """.trimIndent()
        ) { rs ->
            ReporteDto(
                id = rs.getInt("id"),
                usuario = rs.getString("usuario_nombre") ?: rs.getString("username"),
                descripcion = rs.getString("descripcion"),
                dispositivo = rs.getString("dispositivo"),
                creado = rs.getLong("creado")
            )
        }
    }
}

/** COP-RS-06: bitácora de errores y eventos relevantes (tabla bitacora + consola). */
object Bitacora {

    private val log = LoggerFactory.getLogger("COP")

    fun info(mensaje: String) {
        log.info(mensaje)
        guardar("INFO", mensaje)
    }

    fun error(
        mensaje: String,
        causa: Throwable? = null
    ) {
        log.error(mensaje, causa)
        guardar("ERROR", mensaje + (causa?.let { " | ${it::class.simpleName}: ${it.message}" } ?: ""))
    }

    private fun guardar(
        nivel: String,
        mensaje: String
    ) {
        try {
            BaseDatos.insertar(
                "INSERT INTO bitacora (nivel, mensaje, creado) VALUES (?, ?, ?)",
                nivel,
                mensaje.take(1000),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            log.error("No se pudo escribir en la bitácora", e)
        }
    }
}
