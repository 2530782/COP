package com.poligono.cop.data

import com.poligono.cop.models.CitaDto
import com.poligono.cop.models.CitaRegistro
import com.poligono.cop.models.Servicio
import com.poligono.cop.models.UsuarioRegistro
import kotlinx.serialization.json.Json

/**
 * FUENTE DE DATOS: lee los archivos JSON de src/main/resources/data
 * (credenciales predefinidas, servicios y citas) y los convierte en listas de Kotlin.
 *
 * Por ahora la arquitectura es LOCAL (sin base de datos): si cambias un .json,
 * reinicia el servidor para que lo vuelva a leer.
 */
class JsonDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    val usuarios: List<UsuarioRegistro> = leer("data/usuarios.json")
    val servicios: List<Servicio> = leer("data/servicios.json")
    val citas: List<CitaRegistro> = leer("data/citas.json")

    /**
     * Lee un archivo de resources y lo DESERIALIZA (JSON -> objetos Kotlin).
     * "reified T" permite saber en tiempo de ejecución a qué tipo convertir
     * (List<UsuarioRegistro>, List<Servicio>, etc.).
     */
    private inline fun <reified T> leer(ruta: String): T {
        val texto = javaClass.classLoader.getResource(ruta)?.readText(Charsets.UTF_8)
            ?: error("No se encontró el archivo $ruta dentro de src/main/resources")
        return json.decodeFromString<T>(texto)
    }

    /**
     * Regresa las citas (filtradas si se pide) con los nombres resueltos:
     * "FIS" -> "Fisioterapia",  "ana" -> "Ana López".
     */
    fun citasDetalladas(paciente: String?, profesional: String?): List<CitaDto> =
        citas
            .filter { paciente == null || it.paciente == paciente }
            .filter { profesional == null || it.profesional == profesional }
            .map { cita ->
                CitaDto(
                    id = cita.id,
                    servicioId = cita.servicioId,
                    servicio = servicios.firstOrNull { it.id == cita.servicioId }?.nombre ?: cita.servicioId,
                    profesional = cita.profesional,
                    profesionalNombre = nombreDe(cita.profesional),
                    paciente = cita.paciente,
                    pacienteNombre = nombreDe(cita.paciente),
                    fecha = cita.fecha,
                    hora = cita.hora,
                    estado = cita.estado
                )
            }

    private fun nombreDe(username: String): String =
        usuarios.firstOrNull { it.username == username }?.nombre ?: username
}
