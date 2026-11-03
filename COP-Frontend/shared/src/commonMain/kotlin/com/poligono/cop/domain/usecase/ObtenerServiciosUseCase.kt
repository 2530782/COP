package com.poligono.cop.domain.usecase

import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.repository.CopRepository

/** Caso de uso: obtener el catálogo de servicios (COP-RN-03). */
class ObtenerServiciosUseCase(private val repository: CopRepository) {
    suspend operator fun invoke(): Result<List<Servicio>> = repository.obtenerServicios()
}
