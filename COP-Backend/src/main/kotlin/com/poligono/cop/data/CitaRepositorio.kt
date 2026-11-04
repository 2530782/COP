package com.poligono.cop.data

import com.poligono.cop.Configuracion
import com.poligono.cop.models.CitaDto
import com.poligono.cop.models.EstadosCita
import com.poligono.cop.services.Fechas
import java.sql.ResultSet

/** Filtros de la agenda (COP-RN-13). Todos son opcionales. */
data class FiltroCitas(
    val desde: String? = null,
    val hasta: String? = null,
    val servicioId: String? = null,
    val profesional: String? = null,
    val paciente: String? = null,
    val estado: String? = null
)

object CitaRepositorio {

    private const val SELECT = """
        SELECT
            c.id, c.servicio_id, c.profesional, c.paciente, c.nombre_contacto, c.telefono_contacto,
            c.fecha, c.hora, c.estado, c.notas, c.motivo, c.creado_por,
            s.nombre AS servicio_nombre,
            p.nombre AS profesional_nombre,
            u.nombre AS paciente_nombre,
            u.telefono AS paciente_telefono
        FROM citas c
        LEFT JOIN servicios s ON s.id = c.servicio_id
        LEFT JOIN usuarios p ON p.username = c.profesional
        LEFT JOIN usuarios u ON u.username = c.paciente
    """

    private fun mapear(rs: ResultSet): CitaDto {
        val fecha = rs.getString("fecha")
        val hora = rs.getString("hora")
        val estado = rs.getString("estado")

        // COP-RNN-04: solo se puede cambiar con el plazo mínimo de anticipación
        val puedeModificar = estado in EstadosCita.ACTIVOS &&
            Fechas.horasHasta(fecha, hora) >= Configuracion.plazoHorasCambio

        return CitaDto(
            id = rs.getInt("id"),
            servicioId = rs.getString("servicio_id"),
            servicio = rs.getString("servicio_nombre") ?: rs.getString("servicio_id"),
            profesional = rs.getString("profesional"),
            profesionalNombre = rs.getString("profesional_nombre") ?: rs.getString("profesional"),
            paciente = rs.getString("paciente") ?: "",
            pacienteNombre = rs.getString("paciente_nombre") ?: rs.getString("nombre_contacto") ?: "",
            fecha = fecha,
            hora = hora,
            estado = estado,
            pacienteTelefono = rs.getString("paciente_telefono") ?: rs.getString("telefono_contacto") ?: "",
            notas = rs.getString("notas") ?: "",
            motivo = rs.getString("motivo") ?: "",
            creadoPor = rs.getString("creado_por"),
            puedeModificar = puedeModificar
        )
    }

    fun buscar(id: Int): CitaDto? {
        return BaseDatos.consultar("$SELECT WHERE c.id = ?", id) { mapear(it) }.firstOrNull()
    }

    fun listar(filtro: FiltroCitas): List<CitaDto> {
        val condiciones = mutableListOf<String>()
        val parametros = mutableListOf<Any?>()

        if (filtro.desde != null) {
            condiciones.add("c.fecha >= ?")
            parametros.add(filtro.desde)
        }

        if (filtro.hasta != null) {
            condiciones.add("c.fecha <= ?")
            parametros.add(filtro.hasta)
        }

        if (filtro.servicioId != null) {
            condiciones.add("c.servicio_id = ?")
            parametros.add(filtro.servicioId)
        }

        if (filtro.profesional != null) {
            condiciones.add("c.profesional = ?")
            parametros.add(filtro.profesional)
        }

        if (filtro.paciente != null) {
            condiciones.add("c.paciente = ?")
            parametros.add(filtro.paciente)
        }

        if (filtro.estado != null) {
            condiciones.add("c.estado = ?")
            parametros.add(filtro.estado)
        }

        val where = if (condiciones.isEmpty()) "" else "WHERE " + condiciones.joinToString(" AND ")

        return BaseDatos.consultar(
            "$SELECT $where ORDER BY c.fecha, c.hora",
            *parametros.toTypedArray()
        ) { mapear(it) }
    }

    /** Horas ocupadas por citas pendientes o confirmadas del profesional ese día (COP-RNN-01). */
    fun horasOcupadas(
        profesional: String,
        fecha: String,
        excluirCitaId: Int? = null
    ): Set<String> {
        val filas = BaseDatos.consultar(
            """
            SELECT id, hora FROM citas
            WHERE profesional = ? AND fecha = ? AND estado IN ('PENDIENTE', 'CONFIRMADA')
            """.trimIndent(),
            profesional,
            fecha
        ) { rs ->
            Pair(rs.getInt("id"), rs.getString("hora"))
        }

        return filas
            .filter { it.first != excluirCitaId }
            .map { it.second }
            .toSet()
    }

    fun crear(
        servicioId: String,
        profesional: String,
        paciente: String?,
        nombreContacto: String?,
        telefonoContacto: String?,
        fecha: String,
        hora: String,
        estado: String,
        notas: String,
        creadoPor: String
    ): Int {
        val ahora = System.currentTimeMillis()

        return BaseDatos.insertar(
            """
            INSERT INTO citas
                (servicio_id, profesional, paciente, nombre_contacto, telefono_contacto,
                 fecha, hora, estado, notas, motivo, creado_por, creado, actualizado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '', ?, ?, ?)
            """.trimIndent(),
            servicioId,
            profesional,
            paciente,
            nombreContacto,
            telefonoContacto,
            fecha,
            hora,
            estado,
            notas.trim(),
            creadoPor,
            ahora,
            ahora
        )
    }

    fun cambiarEstado(
        id: Int,
        estado: String,
        motivo: String
    ) {
        BaseDatos.ejecutar(
            "UPDATE citas SET estado = ?, motivo = ?, actualizado = ? WHERE id = ?",
            estado,
            motivo.trim(),
            System.currentTimeMillis(),
            id
        )
    }

    fun cambiarFechaHora(
        id: Int,
        fecha: String,
        hora: String,
        estado: String,
        motivo: String
    ) {
        BaseDatos.ejecutar(
            "UPDATE citas SET fecha = ?, hora = ?, estado = ?, motivo = ?, actualizado = ? WHERE id = ?",
            fecha,
            hora,
            estado,
            motivo.trim(),
            System.currentTimeMillis(),
            id
        )
    }
}
