package com.poligono.cop.services

import com.poligono.cop.Configuracion
import io.ktor.http.HttpStatusCode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Fechas {

    val formatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val dias = listOf(
        "lunes",
        "martes",
        "miércoles",
        "jueves",
        "viernes",
        "sábado",
        "domingo"
    )

    private val meses = listOf(
        "enero",
        "febrero",
        "marzo",
        "abril",
        "mayo",
        "junio",
        "julio",
        "agosto",
        "septiembre",
        "octubre",
        "noviembre",
        "diciembre"
    )

    fun ahora(): LocalDateTime {
        return LocalDateTime.now(Configuracion.zona)
    }

    fun hoy(): LocalDate {
        return LocalDate.now(Configuracion.zona)
    }

    fun fecha(texto: String): LocalDate {
        return try {
            LocalDate.parse(texto)
        } catch (e: Exception) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "La fecha debe tener el formato AAAA-MM-DD.")
        }
    }

    fun hora(texto: String): LocalTime {
        return try {
            LocalTime.parse(texto, formatoHora)
        } catch (e: Exception) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "La hora debe tener el formato HH:MM.")
        }
    }

    fun fechaHora(
        fecha: String,
        hora: String
    ): LocalDateTime {
        return LocalDateTime.of(fecha(fecha), hora(hora))
    }

    /** Horas que faltan para la cita (negativo si ya pasó). */
    fun horasHasta(
        fecha: String,
        hora: String
    ): Long {
        return Duration.between(ahora(), fechaHora(fecha, hora)).toHours()
    }

    /** Ejemplo: "lunes 9 de noviembre a las 10:00" */
    fun legible(
        fecha: String,
        hora: String
    ): String {
        val f = fecha(fecha)
        val dia = dias[f.dayOfWeek.value - 1]
        val mes = meses[f.monthValue - 1]
        return "$dia ${f.dayOfMonth} de $mes a las $hora"
    }
}
