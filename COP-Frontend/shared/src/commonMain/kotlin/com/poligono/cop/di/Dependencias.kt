package com.poligono.cop.di

import com.poligono.cop.data.remote.ApiClient
import com.poligono.cop.data.remote.CopApi
import com.poligono.cop.data.remote.urlServidor
import com.poligono.cop.data.repository.CopRepositoryImpl
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.usecase.IniciarSesionUseCase
import com.poligono.cop.domain.usecase.ObtenerCitasUseCase
import com.poligono.cop.domain.usecase.ObtenerProfesionalesUseCase
import com.poligono.cop.domain.usecase.ObtenerServiciosUseCase

/**
 * "Inyección de dependencias" manual: aquí se ARMAN todas las piezas una sola vez
 * y en orden:  HttpClient -> CopApi -> Repository -> Casos de uso.
 * Los ViewModels piden sus casos de uso a este objeto.
 *
 * "by lazy" = se crea hasta la primera vez que se usa.
 */
object Dependencias {
    private val api: CopApi by lazy { CopApi(ApiClient.httpClient, urlServidor()) }
    private val repository: CopRepository by lazy { CopRepositoryImpl(api) }

    val iniciarSesion by lazy { IniciarSesionUseCase(repository) }
    val obtenerServicios by lazy { ObtenerServiciosUseCase(repository) }
    val obtenerProfesionales by lazy { ObtenerProfesionalesUseCase(repository) }
    val obtenerCitas by lazy { ObtenerCitasUseCase(repository) }
}
