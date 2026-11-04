package com.poligono.cop.domain.repository

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

/**
 * CONTRATO (interfaz) de lo que la app necesita de los datos.
 * La capa domain solo dice QUÉ se necesita; la capa data dice CÓMO se obtiene
 * (en este caso con Ktor Client contra el backend, ver CopRepositoryImpl).
 *
 * Todas las funciones de red son "suspend" porque no deben bloquear la pantalla
 * mientras esperan. Devuelven Result<T>: o trae el dato (éxito) o trae el error (fallo).
 */
interface CopRepository {

    // ---------- Sesión ----------
    suspend fun iniciarSesion(username: String, password: String): Result<Usuario>
    suspend fun registrar(
        username: String,
        password: String,
        nombre: String,
        telefono: String,
        correo: String,
        aceptaPrivacidad: Boolean
    ): Result<Usuario>
    suspend fun recuperarContrasena(username: String): Result<String>
    suspend fun restablecerContrasena(username: String, codigo: String, nueva: String): Result<String>

    /** Usuario de la sesión guardada en el teléfono (para no pedir la contraseña cada vez). */
    fun sesionGuardada(): Usuario?
    fun cerrarSesion()

    // ---------- Catálogo ----------
    suspend fun obtenerConfiguracion(): Result<Configuracion>
    suspend fun obtenerServicios(): Result<List<Servicio>>
    suspend fun obtenerProfesionales(servicioId: String? = null, todos: Boolean = false): Result<List<Profesional>>
    suspend fun obtenerPoligonos(): Result<List<Poligono>>
    fun poligonosGuardados(): List<Poligono>

    // ---------- Citas ----------
    suspend fun obtenerCitas(paciente: String? = null, profesional: String? = null): Result<List<Cita>>
    /** Citas descargadas antes, para verlas sin internet (COP-RNN-06), y cuándo se guardaron. */
    fun citasGuardadas(): Pair<List<Cita>, Long>?
    suspend fun fechasDisponibles(profesional: String): Result<List<FechaDisponible>>
    suspend fun horasLibres(profesional: String, fecha: String, excluirCitaId: Int? = null): Result<List<String>>
    suspend fun solicitarCita(profesional: String, fecha: String, hora: String, notas: String): Result<Cita>
    suspend fun registrarCitaManual(
        profesional: String,
        fecha: String,
        hora: String,
        nombre: String,
        telefono: String,
        notas: String
    ): Result<Cita>
    suspend fun cancelarCita(id: Int): Result<Cita>
    suspend fun reprogramarCita(id: Int, fecha: String, hora: String, motivo: String): Result<Cita>
    suspend fun cambiarEstadoCita(id: Int, estado: String, motivo: String): Result<Cita>

    // ---------- Avisos ----------
    suspend fun obtenerNotificaciones(): Result<List<Notificacion>>
    suspend fun marcarNotificacionLeida(id: Int): Result<Unit>
    suspend fun marcarTodasLeidas(): Result<Unit>

    // ---------- Administración ----------
    suspend fun guardarServicio(servicio: Servicio, esNuevo: Boolean): Result<Unit>
    suspend fun guardarProfesional(
        username: String,
        password: String,
        nombre: String,
        servicioId: String,
        activo: Boolean,
        esNuevo: Boolean
    ): Result<Unit>
    suspend fun obtenerHorarios(profesional: String): Result<List<Horario>>
    suspend fun agregarHorario(profesional: String, diaSemana: Int, inicio: String, fin: String): Result<List<Horario>>
    suspend fun eliminarHorario(id: Int): Result<Unit>
    suspend fun enviarReporte(descripcion: String, dispositivo: String): Result<String>
    suspend fun obtenerReportes(): Result<List<Reporte>>
    fun urlExportar(tipo: String, desde: String, hasta: String, servicioId: String?): String
}
