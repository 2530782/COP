package com.poligono.cop.data

import com.poligono.cop.models.HorarioDto
import com.poligono.cop.models.Servicio
import java.sql.ResultSet

/** Servicios del Centro (fisioterapia, nutrición, psicología...). */
object ServicioRepositorio {

    private fun mapear(rs: ResultSet): Servicio {
        return Servicio(
            id = rs.getString("id"),
            nombre = rs.getString("nombre"),
            descripcion = rs.getString("descripcion"),
            duracionMinutos = rs.getInt("duracion_minutos"),
            activo = rs.getBoolean("activo")
        )
    }

    fun listar(): List<Servicio> {
        return BaseDatos.consultar("SELECT * FROM servicios ORDER BY nombre") { mapear(it) }
    }

    fun buscar(id: String): Servicio? {
        return BaseDatos.consultar(
            "SELECT * FROM servicios WHERE id = ?",
            id
        ) { mapear(it) }.firstOrNull()
    }

    fun crear(s: Servicio) {
        BaseDatos.ejecutar(
            "INSERT INTO servicios (id, nombre, descripcion, duracion_minutos, activo) VALUES (?, ?, ?, ?, ?)",
            s.id.trim().uppercase(),
            s.nombre.trim(),
            s.descripcion.trim(),
            s.duracionMinutos,
            s.activo
        )
    }

    fun actualizar(s: Servicio) {
        BaseDatos.ejecutar(
            "UPDATE servicios SET nombre = ?, descripcion = ?, duracion_minutos = ?, activo = ? WHERE id = ?",
            s.nombre.trim(),
            s.descripcion.trim(),
            s.duracionMinutos,
            s.activo,
            s.id
        )
    }
}

/** Disponibilidad semanal de cada profesional (día 1 = lunes ... 7 = domingo). */
object HorarioRepositorio {

    private fun mapear(rs: ResultSet): HorarioDto {
        return HorarioDto(
            id = rs.getInt("id"),
            profesional = rs.getString("profesional"),
            diaSemana = rs.getInt("dia_semana"),
            horaInicio = rs.getString("hora_inicio"),
            horaFin = rs.getString("hora_fin")
        )
    }

    fun delProfesional(profesional: String): List<HorarioDto> {
        return BaseDatos.consultar(
            "SELECT * FROM horarios WHERE profesional = ? ORDER BY dia_semana, hora_inicio",
            profesional
        ) { mapear(it) }
    }

    fun crear(
        profesional: String,
        diaSemana: Int,
        horaInicio: String,
        horaFin: String
    ) {
        BaseDatos.insertar(
            "INSERT INTO horarios (profesional, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)",
            profesional,
            diaSemana,
            horaInicio,
            horaFin
        )
    }

    fun eliminar(id: Int): Int {
        return BaseDatos.ejecutar("DELETE FROM horarios WHERE id = ?", id)
    }
}
