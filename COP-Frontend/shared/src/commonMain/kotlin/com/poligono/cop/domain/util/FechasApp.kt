package com.poligono.cop.domain.util

import com.poligono.cop.plataforma.ahoraMillis
import com.poligono.cop.plataforma.desfaseZonaMinutos

/**
 * Manejo de fechas sin librerías externas. Las fechas viajan como "AAAA-MM-DD"
 * y las horas como "HH:MM", igual que en el servidor.
 */
object FechasApp {

    private const val MS_DIA = 86_400_000L

    private val diasCortos = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

    private val diasLargos = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

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

    private fun localMillis(): Long {
        return ahoraMillis() + desfaseZonaMinutos() * 60_000L
    }

    fun hoy(): String {
        return desdeDias(localMillis() / MS_DIA)
    }

    fun horaActual(): String {
        val minutosDelDia = (localMillis() % MS_DIA) / 60_000
        return dos(minutosDelDia / 60) + ":" + dos(minutosDelDia % 60)
    }

    fun sumarDias(
        fecha: String,
        dias: Int
    ): String {
        return desdeDias(aDias(fecha) + dias)
    }

    /** 1 = lunes ... 7 = domingo */
    fun diaSemana(fecha: String): Int {
        val dias = aDias(fecha)
        return (((dias + 3) % 7 + 7) % 7 + 1).toInt()
    }

    /** Momento de la cita en milisegundos UTC, usando la zona del teléfono. */
    fun millisDe(
        fecha: String,
        hora: String
    ): Long {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        val local = aDias(fecha) * MS_DIA + h * 3_600_000L + m * 60_000L

        return local - desfaseZonaMinutos() * 60_000L
    }

    /** Ejemplo: "lun 9 nov" */
    fun corta(fecha: String): String {
        val (_, mes, dia) = partes(fecha)
        return "${diasCortos[diaSemana(fecha) - 1]} $dia ${meses[mes - 1].take(3)}"
    }

    /** Ejemplo: "lunes 9 de noviembre" */
    fun larga(fecha: String): String {
        val (_, mes, dia) = partes(fecha)
        return "${diasLargos[diaSemana(fecha) - 1]} $dia de ${meses[mes - 1]}"
    }

    fun nombreDia(diaSemana: Int): String {
        return diasLargos.getOrElse(diaSemana - 1) { "?" }.replaceFirstChar { it.uppercase() }
    }

    /** Ejemplo: "09/11 14:05" a partir de milisegundos. */
    fun momento(millis: Long): String {
        val local = millis + desfaseZonaMinutos() * 60_000L
        val (_, mes, dia) = partes(desdeDias(local / MS_DIA))
        val minutos = (local % MS_DIA) / 60_000

        return "${dos(dia.toLong())}/${dos(mes.toLong())} ${dos(minutos / 60)}:${dos(minutos % 60)}"
    }

    fun esValida(fecha: String): Boolean {
        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        if (!regex.matches(fecha)) {
            return false
        }

        val (_, mes, dia) = partes(fecha)
        return mes in 1..12 && dia in 1..31 && desdeDias(aDias(fecha)) == fecha
    }

    // ---------- algoritmo de días civiles (H. Hinnant) ----------

    private fun partes(fecha: String): Triple<Int, Int, Int> {
        val p = fecha.split("-")
        return Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }

    private fun aDias(fecha: String): Long {
        val (anio, mes, dia) = partes(fecha)
        val y = if (mes <= 2) anio - 1 else anio
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = if (mes > 2) mes - 3 else mes + 9
        val doy = (153 * mp + 2) / 5 + dia - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy

        return era * 146_097L + doe - 719_468L
    }

    private fun desdeDias(diasEpoch: Long): String {
        val z = diasEpoch + 719_468L
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val dia = doy - (153 * mp + 2) / 5 + 1
        val mes = if (mp < 10) mp + 3 else mp - 9
        val anio = yoe + era * 400 + (if (mes <= 2) 1 else 0)

        return "$anio-${dos(mes)}-${dos(dia)}"
    }

    private fun dos(numero: Long): String {
        return numero.toString().padStart(2, '0')
    }
}
