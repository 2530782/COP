package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository

/**
 * Caso de uso: obtener las citas que le corresponden a cada rol.
 * Confidencialidad (COP-RC-04): cada quien ve solo lo que le toca.
 *  - USUARIO       -> solo SUS citas (como paciente)
 *  - PROFESIONAL   -> solo las citas que él/ella atiende
 *  - PERSONAL y ADMINISTRADOR -> la agenda completa
 */
class ObtenerCitasUseCase(private val repository: CopRepository) {

    suspend operator fun invoke(usuario: Usuario): Result<List<Cita>> =
        when (usuario.rol) {
            Rol.USUARIO -> repository.obtenerCitas(paciente = usuario.username)
            Rol.PROFESIONAL -> repository.obtenerCitas(profesional = usuario.username)
            Rol.PERSONAL, Rol.ADMINISTRADOR -> repository.obtenerCitas()
        }.map { citas ->
            // Ordenamos por fecha y hora (el texto "2026-10-12" + "10:00" se ordena bien alfabéticamente)
            citas.sortedWith(compareBy({ it.fecha }, { it.hora }))
        }
}
