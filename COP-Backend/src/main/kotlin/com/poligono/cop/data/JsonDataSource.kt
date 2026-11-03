package com.poligono.cop.data

import com.poligono.cop.models.CitaRegistro
import com.poligono.cop.models.Servicio
import com.poligono.cop.models.UsuarioRegistro
import kotlinx.serialization.json.Json

/**
 * Lee los archivos JSON de src/main/resources/data (usuarios, servicios y citas).
 *
 * Antes eran la única fuente de datos. Ahora son los DATOS INICIALES:
 * la primera vez que arranca el servidor se copian a la base de datos
 * (ver Esquema.cargarDatosIniciales). Después, todo se guarda en la base.
 */
class JsonDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    val usuarios: List<UsuarioRegistro> = leer("data/usuarios.json")
    val servicios: List<Servicio> = leer("data/servicios.json")
    val citas: List<CitaRegistro> = leer("data/citas.json")

    /**
     * Lee un archivo de resources y lo DESERIALIZA (JSON -> objetos Kotlin).
     * "reified T" permite saber en tiempo de ejecución a qué tipo convertir.
     */
    private inline fun <reified T> leer(ruta: String): T {
        val texto = javaClass.classLoader.getResource(ruta)?.readText(Charsets.UTF_8)
            ?: error("No se encontró el archivo $ruta dentro de src/main/resources")
        return json.decodeFromString<T>(texto)
    }
}
