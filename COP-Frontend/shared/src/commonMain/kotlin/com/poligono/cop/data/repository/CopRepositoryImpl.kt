package com.poligono.cop.data.repository

import com.poligono.cop.data.dto.CambioEstadoRequest
import com.poligono.cop.data.dto.CitaDto
import com.poligono.cop.data.dto.CitaManualRequest
import com.poligono.cop.data.dto.CitaRequest
import com.poligono.cop.data.dto.ConfiguracionDto
import com.poligono.cop.data.dto.FechaDisponibleDto
import com.poligono.cop.data.dto.HorarioDto
import com.poligono.cop.data.dto.HorarioRequest
import com.poligono.cop.data.dto.LoginRequest
import com.poligono.cop.data.dto.LoginResponse
import com.poligono.cop.data.dto.NotificacionDto
import com.poligono.cop.data.dto.PoligonoDto
import com.poligono.cop.data.dto.ProfesionalDto
import com.poligono.cop.data.dto.ProfesionalRequest
import com.poligono.cop.data.dto.RecuperarRequest
import com.poligono.cop.data.dto.RecuperarResponse
import com.poligono.cop.data.dto.RegistroRequest
import com.poligono.cop.data.dto.ReporteDto
import com.poligono.cop.data.dto.ReporteRequest
import com.poligono.cop.data.dto.ReprogramarRequest
import com.poligono.cop.data.dto.RespuestaError
import com.poligono.cop.data.dto.RespuestaOk
import com.poligono.cop.data.dto.RestablecerRequest
import com.poligono.cop.data.dto.ServicioDto
import com.poligono.cop.data.local.AlmacenLocal
import com.poligono.cop.data.local.PoligonosRespaldo
import com.poligono.cop.data.mapper.aDominio
import com.poligono.cop.data.mapper.aDto
import com.poligono.cop.data.remote.CopApi
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Configuracion
import com.poligono.cop.domain.model.FechaDisponible
import com.poligono.cop.domain.model.Horario
import com.poligono.cop.domain.model.Notificacion
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Reporte
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación REAL del contrato CopRepository (capa domain).
 * Usa CopApi (Ktor Client) para hablar con el backend, convierte los DTOs en
 * modelos de dominio con los mappers y guarda en el teléfono lo necesario
 * para trabajar sin internet (AlmacenLocal).
 */
class CopRepositoryImpl(private val api: CopApi) : CopRepository {

    /** Token de la sesión actual. CopApi lo lee para mandarlo en cada petición. */
    var token: String? = null
        private set

    /** Se llama cuando el servidor responde 401 con sesión iniciada (token vencido). */
    var alExpirarSesion: (() -> Unit)? = null

    // ---------- Sesión ----------

    override suspend fun iniciarSesion(username: String, password: String): Result<Usuario> =
        peticionSegura {
            val cuerpo: LoginResponse = leer { api.login(LoginRequest(username, password)) }
            guardarSesion(cuerpo)
        }

    override suspend fun registrar(
        username: String,
        password: String,
        nombre: String,
        telefono: String,
        correo: String,
        aceptaPrivacidad: Boolean
    ): Result<Usuario> =
        peticionSegura {
            val peticion = RegistroRequest(username, password, nombre, telefono, correo, aceptaPrivacidad)
            val cuerpo: LoginResponse = leer { api.registro(peticion) }
            guardarSesion(cuerpo)
        }

    override suspend fun recuperarContrasena(username: String): Result<String> =
        peticionSegura {
            val cuerpo: RecuperarResponse = leer { api.recuperar(RecuperarRequest(username.trim())) }
            val codigo = cuerpo.codigoDemo

            if (codigo != null) {
                "${cuerpo.mensaje}\n\nModo demostración: tu código es $codigo"
            } else {
                cuerpo.mensaje
            }
        }

    override suspend fun restablecerContrasena(username: String, codigo: String, nueva: String): Result<String> =
        peticionSegura {
            val cuerpo: RespuestaOk = leer { api.restablecer(RestablecerRequest(username.trim(), codigo, nueva)) }
            cuerpo.mensaje
        }

    override fun sesionGuardada(): Usuario? {
        val sesion = AlmacenLocal.leerSesion() ?: return null
        val usuario = sesion.usuario ?: return null

        token = sesion.token
        return usuario.aDominio()
    }

    override fun cerrarSesion() {
        token = null
        AlmacenLocal.borrarSesion()
    }

    private fun guardarSesion(cuerpo: LoginResponse): Usuario {
        val usuarioDto = cuerpo.usuario

        if (!cuerpo.exito || usuarioDto == null) {
            throw ErrorDelServidor(cuerpo.mensaje)
        }

        token = cuerpo.token
        AlmacenLocal.guardarSesion(cuerpo)
        return usuarioDto.aDominio()
    }

    // ---------- Catálogo ----------

    override suspend fun obtenerConfiguracion(): Result<Configuracion> =
        peticionSegura {
            val dto: ConfiguracionDto = leer { api.configuracion() }
            AlmacenLocal.guardarConfiguracion(dto)
            dto.aDominio()
        }

    override suspend fun obtenerServicios(): Result<List<Servicio>> =
        peticionSegura {
            val lista: List<ServicioDto> = leer { api.servicios() }
            lista.map { it.aDominio() }
        }

    override suspend fun obtenerProfesionales(servicioId: String?, todos: Boolean): Result<List<Profesional>> =
        peticionSegura {
            val lista: List<ProfesionalDto> = leer { api.profesionales(servicioId, todos) }
            lista.map { it.aDominio() }
        }

    override suspend fun obtenerPoligonos(): Result<List<Poligono>> =
        peticionSegura {
            val lista: List<PoligonoDto> = leer { api.poligonos() }
            AlmacenLocal.guardarPoligonos(lista)
            lista.map { it.aDominio() }
        }

    override fun poligonosGuardados(): List<Poligono> =
        (AlmacenLocal.leerPoligonos() ?: PoligonosRespaldo.lista).map { it.aDominio() }

    // ---------- Citas ----------

    override suspend fun obtenerCitas(paciente: String?, profesional: String?): Result<List<Cita>> =
        peticionSegura {
            val lista: List<CitaDto> = leer { api.citas(paciente, profesional) }
            AlmacenLocal.guardarCitas(lista)
            lista.map { it.aDominio() }
        }

    override fun citasGuardadas(): Pair<List<Cita>, Long>? {
        val lista = AlmacenLocal.leerCitas() ?: return null
        return Pair(lista.map { it.aDominio() }, AlmacenLocal.fechaCitas())
    }

    override suspend fun fechasDisponibles(profesional: String): Result<List<FechaDisponible>> =
        peticionSegura {
            val lista: List<FechaDisponibleDto> = leer { api.fechasDisponibles(profesional) }
            lista.map { it.aDominio() }
        }

    override suspend fun horasLibres(profesional: String, fecha: String, excluirCitaId: Int?): Result<List<String>> =
        peticionSegura { leer<List<String>> { api.horasLibres(profesional, fecha, excluirCitaId) } }

    override suspend fun solicitarCita(profesional: String, fecha: String, hora: String, notas: String): Result<Cita> =
        peticionSegura {
            val dto: CitaDto = leer { api.solicitarCita(CitaRequest(profesional, fecha, hora, notas)) }
            dto.aDominio()
        }

    override suspend fun registrarCitaManual(
        profesional: String,
        fecha: String,
        hora: String,
        nombre: String,
        telefono: String,
        notas: String
    ): Result<Cita> =
        peticionSegura {
            val peticion = CitaManualRequest(profesional, fecha, hora, nombre, telefono, notas)
            val dto: CitaDto = leer { api.citaManual(peticion) }
            dto.aDominio()
        }

    override suspend fun cancelarCita(id: Int): Result<Cita> =
        peticionSegura {
            val dto: CitaDto = leer { api.cancelarCita(id) }
            dto.aDominio()
        }

    override suspend fun reprogramarCita(id: Int, fecha: String, hora: String, motivo: String): Result<Cita> =
        peticionSegura {
            val dto: CitaDto = leer { api.reprogramarCita(id, ReprogramarRequest(fecha, hora, motivo)) }
            dto.aDominio()
        }

    override suspend fun cambiarEstadoCita(id: Int, estado: String, motivo: String): Result<Cita> =
        peticionSegura {
            val dto: CitaDto = leer { api.cambiarEstado(id, CambioEstadoRequest(estado, motivo)) }
            dto.aDominio()
        }

    // ---------- Avisos ----------

    override suspend fun obtenerNotificaciones(): Result<List<Notificacion>> =
        peticionSegura {
            val lista: List<NotificacionDto> = leer { api.notificaciones() }
            lista.map { it.aDominio() }
        }

    override suspend fun marcarNotificacionLeida(id: Int): Result<Unit> =
        peticionSegura {
            leer<RespuestaOk> { api.marcarLeida(id) }
            Unit
        }

    override suspend fun marcarTodasLeidas(): Result<Unit> =
        peticionSegura {
            leer<RespuestaOk> { api.marcarTodasLeidas() }
            Unit
        }

    // ---------- Administración ----------

    override suspend fun guardarServicio(servicio: Servicio, esNuevo: Boolean): Result<Unit> =
        peticionSegura {
            if (esNuevo) {
                leer<List<ServicioDto>> { api.crearServicio(servicio.aDto()) }
            } else {
                leer<List<ServicioDto>> { api.actualizarServicio(servicio.aDto()) }
            }
            Unit
        }

    override suspend fun guardarProfesional(
        username: String,
        password: String,
        nombre: String,
        servicioId: String,
        activo: Boolean,
        esNuevo: Boolean
    ): Result<Unit> =
        peticionSegura {
            val peticion = ProfesionalRequest(username, password, nombre, servicioId, activo)

            if (esNuevo) {
                leer<List<ProfesionalDto>> { api.crearProfesional(peticion) }
            } else {
                leer<List<ProfesionalDto>> { api.actualizarProfesional(peticion) }
            }
            Unit
        }

    override suspend fun obtenerHorarios(profesional: String): Result<List<Horario>> =
        peticionSegura {
            val lista: List<HorarioDto> = leer { api.horarios(profesional) }
            lista.map { it.aDominio() }
        }

    override suspend fun agregarHorario(profesional: String, diaSemana: Int, inicio: String, fin: String): Result<List<Horario>> =
        peticionSegura {
            val lista: List<HorarioDto> = leer { api.crearHorario(HorarioRequest(profesional, diaSemana, inicio, fin)) }
            lista.map { it.aDominio() }
        }

    override suspend fun eliminarHorario(id: Int): Result<Unit> =
        peticionSegura {
            leer<RespuestaOk> { api.eliminarHorario(id) }
            Unit
        }

    override suspend fun enviarReporte(descripcion: String, dispositivo: String): Result<String> =
        peticionSegura {
            val cuerpo: RespuestaOk = leer { api.enviarReporte(ReporteRequest(descripcion, dispositivo)) }
            cuerpo.mensaje
        }

    override suspend fun obtenerReportes(): Result<List<Reporte>> =
        peticionSegura {
            val lista: List<ReporteDto> = leer { api.reportes() }
            lista.map { it.aDominio() }
        }

    override fun urlExportar(tipo: String, desde: String, hasta: String, servicioId: String?): String =
        api.urlExportar(tipo, desde, hasta, servicioId)

    // ---------- auxiliares ----------

    /**
     * Lee el JSON de la respuesta si salió bien (200, 201).
     * Si no, lanza ErrorDelServidor con el "mensaje" que mandó el backend.
     */
    private suspend inline fun <reified T> leer(llamada: () -> HttpResponse): T {
        val respuesta = llamada()

        if (respuesta.status.isSuccess()) {
            return respuesta.body()
        }

        val mensaje = try {
            respuesta.body<RespuestaError>().mensaje
        } catch (e: Exception) {
            "Ocurrió un error en el servidor (${respuesta.status.value})."
        }

        // 401 con sesión iniciada = el token venció: la app regresa al login
        if (respuesta.status.value == 401 && token != null) {
            alExpirarSesion?.invoke()
        }

        throw ErrorDelServidor(mensaje)
    }

    /**
     * Ejecuta un bloque de red y lo envuelve en Result:
     *  - si todo sale bien           -> Result.success(dato)
     *  - si el servidor dio un error -> Result.failure(con el mensaje del servidor)
     *  - si no hay conexión          -> Result.failure(SinConexion)
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
            Result.failure(SinConexion("$MENSAJE_SIN_CONEXION\n(${e::class.simpleName}: ${e.message})"))
        }

    /** Error con un mensaje que viene del backend y que se puede mostrar tal cual al usuario. */
    private class ErrorDelServidor(mensaje: String) : Exception(mensaje)

    private companion object {
        const val MENSAJE_SIN_CONEXION =
            "No se pudo conectar con el servidor COP. Revisa tu internet o que el backend esté encendido."
    }
}

/** El teléfono no pudo llegar al servidor (sin internet o backend apagado). */
class SinConexion(mensaje: String) : Exception(mensaje)
