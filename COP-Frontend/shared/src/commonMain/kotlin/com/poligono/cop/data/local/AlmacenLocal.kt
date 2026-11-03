package com.poligono.cop.data.local

import com.poligono.cop.data.dto.CitaDto
import com.poligono.cop.data.dto.ConfiguracionDto
import com.poligono.cop.data.dto.LoginResponse
import com.poligono.cop.data.dto.PoligonoDto
import com.poligono.cop.plataforma.ahoraMillis
import com.poligono.cop.plataforma.borrarLocal
import com.poligono.cop.plataforma.guardarLocal
import com.poligono.cop.plataforma.leerLocal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * ALMACENAMIENTO LOCAL (persistencia en el teléfono).
 * Android: SharedPreferences   ·   iOS: NSUserDefaults   (ver plataforma/Plataforma.kt)
 *
 * Se guarda:
 *  - la sesión (usuario + token), para no pedir la contraseña cada vez (COP-RN-02)
 *  - las citas descargadas, para consultarlas sin internet (COP-RNN-06)
 *  - los polígonos y la configuración, para el mapa sin internet
 *  - qué avisos ya se mostraron como notificación
 *
 * Las credenciales de la huella las guarda aparte AccesoBiometricoAndroid.
 */
object AlmacenLocal {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private const val SESION = "sesion"
    private const val CITAS = "citas"
    private const val CITAS_FECHA = "citas_fecha"
    private const val POLIGONOS = "poligonos"
    private const val CONFIGURACION = "configuracion"
    private const val AVISOS_MOSTRADOS = "avisos_mostrados"

    // ---------- Sesión ----------

    fun guardarSesion(respuesta: LoginResponse) {
        escribir(SESION, LoginResponse.serializer(), respuesta)
    }

    fun leerSesion(): LoginResponse? = leer(SESION, LoginResponse.serializer())

    fun borrarSesion() {
        borrarLocal(SESION)
        borrarLocal(CITAS)
        borrarLocal(CITAS_FECHA)
        borrarLocal(AVISOS_MOSTRADOS)
    }

    // ---------- Citas sin conexión ----------

    fun guardarCitas(citas: List<CitaDto>) {
        escribir(CITAS, ListSerializer(CitaDto.serializer()), citas)
        guardarLocal(CITAS_FECHA, ahoraMillis().toString())
    }

    fun leerCitas(): List<CitaDto>? = leer(CITAS, ListSerializer(CitaDto.serializer()))

    fun fechaCitas(): Long = leerLocal(CITAS_FECHA)?.toLongOrNull() ?: 0L

    // ---------- Polígonos y configuración ----------

    fun guardarPoligonos(lista: List<PoligonoDto>) {
        escribir(POLIGONOS, ListSerializer(PoligonoDto.serializer()), lista)
    }

    fun leerPoligonos(): List<PoligonoDto>? = leer(POLIGONOS, ListSerializer(PoligonoDto.serializer()))

    fun guardarConfiguracion(configuracion: ConfiguracionDto) {
        escribir(CONFIGURACION, ConfiguracionDto.serializer(), configuracion)
    }

    fun leerConfiguracion(): ConfiguracionDto = leer(CONFIGURACION, ConfiguracionDto.serializer()) ?: ConfiguracionDto()

    // ---------- Avisos ya mostrados ----------

    fun avisosMostrados(): Set<Int> = leer(AVISOS_MOSTRADOS, ListSerializer(Int.serializer()))?.toSet() ?: emptySet()

    fun guardarAvisosMostrados(ids: Set<Int>) {
        // Solo los últimos 200 para que no crezca sin límite
        escribir(AVISOS_MOSTRADOS, ListSerializer(Int.serializer()), ids.sorted().takeLast(200))
    }

    // ---------- auxiliares: objeto <-> texto JSON ----------

    private fun <T> escribir(clave: String, serializador: KSerializer<T>, valor: T) {
        guardarLocal(clave, json.encodeToString(serializador, valor))
    }

    private fun <T> leer(clave: String, serializador: KSerializer<T>): T? {
        val texto = leerLocal(clave) ?: return null

        return try {
            json.decodeFromString(serializador, texto)
        } catch (e: Exception) {
            null
        }
    }
}
