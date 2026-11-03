package com.poligono.cop.domain.repository

import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario

/**
 * CONTRATO (interfaz) de lo que la app necesita de los datos.
 * La capa domain solo dice QUÉ se necesita; la capa data dice CÓMO se obtiene
 * (en este caso con Ktor Client contra el backend, ver CopRepositoryImpl).
 *
 * Todas las funciones son "suspend" porque hacen una petición de red
 * y no deben bloquear la pantalla mientras esperan.
 * Devuelven Result<T>: o trae el dato (éxito) o trae el error (fallo).
 */
interface CopRepository {
    suspend fun iniciarSesion(username: String, password: String): Result<Usuario>
    suspend fun obtenerServicios(): Result<List<Servicio>>
    suspend fun obtenerProfesionales(): Result<List<Profesional>>
    suspend fun obtenerCitas(paciente: String? = null, profesional: String? = null): Result<List<Cita>>
}
