package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.repository.CopRepository

/**
 * Caso de uso: solicitar una cita (COP-RN-05) o registrarla por teléfono (COP-RN-15).
 * Revisa que estén todos los datos antes de ir al servidor (COP-RNN-09).
 * El servidor revisa además que el horario siga libre (COP-RNN-01 y 03).
 */
class AgendarCitaUseCase(private val repository: CopRepository) {

    suspend operator fun invoke(
        profesional: String?,
        fecha: String?,
        hora: String?,
        notas: String
    ): Result<Cita> {
        if (profesional == null || fecha == null || hora == null) {
            return Result.failure(IllegalArgumentException("Elige profesional, día y hora."))
        }

        if (notas.length > 500) {
            return Result.failure(IllegalArgumentException("El comentario no debe pasar de 500 caracteres."))
        }

        return repository.solicitarCita(profesional, fecha, hora, notas.trim())
    }

    /** Cita presencial o telefónica registrada por el personal. */
    suspend fun manual(
        profesional: String?,
        fecha: String?,
        hora: String?,
        nombre: String,
        telefono: String,
        notas: String
    ): Result<Cita> {
        val error = Validar.nombre(nombre) ?: Validar.telefono(telefono)

        if (error != null) {
            return Result.failure(IllegalArgumentException(error))
        }

        if (profesional == null || fecha == null || hora == null) {
            return Result.failure(IllegalArgumentException("Elige servicio, profesional, día y hora."))
        }

        return repository.registrarCitaManual(profesional, fecha, hora, nombre.trim(), telefono, notas.trim())
    }
}
