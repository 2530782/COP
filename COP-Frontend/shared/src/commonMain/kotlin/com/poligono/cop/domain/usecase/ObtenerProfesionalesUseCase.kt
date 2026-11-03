package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.repository.CopRepository

/** Caso de uso: obtener la lista de profesionales (COP-RN-04 / COP-RN-17). */
class ObtenerProfesionalesUseCase(private val repository: CopRepository) {
    suspend operator fun invoke(): Result<List<Profesional>> = repository.obtenerProfesionales()
}
