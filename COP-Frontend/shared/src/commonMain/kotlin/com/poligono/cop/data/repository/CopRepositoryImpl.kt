package com.poligono.cop.data.repository

import com.poligono.cop.data.dto.LoginRequest
import com.poligono.cop.data.dto.LoginResponse
import com.poligono.cop.data.mapper.aDominio
import com.poligono.cop.data.remote.CopApi
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación REAL del contrato CopRepository (capa domain).
 * Usa CopApi (Ktor Client) para hablar con el backend y convierte
 * los DTOs en modelos de dominio con los mappers.
 */
class CopRepositoryImpl(private val api: CopApi) : CopRepository {

    override suspend fun iniciarSesion(username: String, password: String): Result<Usuario> =
        peticionSegura {
            val respuesta = api.login(LoginRequest(username, password))
            val cuerpo: LoginResponse = respuesta.body()

            // 200 OK + exito = true  -> sesión iniciada
            // 401 (contraseña mal) o 423 (cuenta bloqueada) -> usamos el mensaje del servidor
            val usuarioDto = cuerpo.usuario
            if (respuesta.status == HttpStatusCode.OK && cuerpo.exito && usuarioDto != null) {
                usuarioDto.aDominio()
            } else {
                throw ErrorDelServidor(cuerpo.mensaje)
            }
        }

    override suspend fun obtenerServicios(): Result<List<Servicio>> =
        peticionSegura { api.servicios().map { it.aDominio() } }

    override suspend fun obtenerProfesionales(): Result<List<Profesional>> =
        peticionSegura { api.profesionales().map { it.aDominio() } }

    override suspend fun obtenerCitas(paciente: String?, profesional: String?): Result<List<Cita>> =
        peticionSegura { api.citas(paciente, profesional).map { it.aDominio() } }

    /**
     * Ejecuta un bloque de red y lo envuelve en Result:
     *  - si todo sale bien           -> Result.success(dato)
     *  - si el servidor dio un error -> Result.failure(con el mensaje del servidor)
     *  - si no hay conexión          -> Result.failure("No se pudo conectar...")
     * La CancellationException se vuelve a lanzar porque es la forma en que
     * las corrutinas se cancelan (ej. al salir de la pantalla) y no es un error real.
     */
    private suspend fun <T> peticionSegura(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorDelServidor) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("$MENSAJE_SIN_CONEXION\n(${e::class.simpleName}: ${e.message})", e))
        }

    /** Error con un mensaje que viene del backend y que se puede mostrar tal cual al usuario. */
    private class ErrorDelServidor(mensaje: String) : Exception(mensaje)

    private companion object {
        const val MENSAJE_SIN_CONEXION =
            "No se pudo conectar con el servidor COP. Revisa que el backend esté encendido."
    }
}
