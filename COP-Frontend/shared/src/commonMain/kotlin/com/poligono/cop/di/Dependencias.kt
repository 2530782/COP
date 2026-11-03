package com.poligono.cop.di

import com.poligono.cop.data.remote.ApiClient
import com.poligono.cop.data.remote.CopApi
import com.poligono.cop.data.remote.urlServidor
import com.poligono.cop.data.repository.CopRepositoryImpl
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.domain.usecase.AgendarCitaUseCase
import com.poligono.cop.domain.usecase.IniciarSesionUseCase
import com.poligono.cop.domain.usecase.ObtenerCitasUseCase
import com.poligono.cop.domain.usecase.ObtenerProfesionalesUseCase
import com.poligono.cop.domain.usecase.ObtenerServiciosUseCase
import com.poligono.cop.domain.usecase.PoligonosCercanosUseCase
import com.poligono.cop.domain.usecase.RegistrarUsuarioUseCase

/**
 * "Inyección de dependencias" manual: aquí se ARMAN todas las piezas una sola vez
 * y en orden:  HttpClient -> CopApi -> Repository -> Casos de uso.
 * Los ViewModels piden sus casos de uso (o el repositorio, para consultas simples) a este objeto.
 *
 * "by lazy" = se crea hasta la primera vez que se usa.
 */
object Dependencias {
    private val repositorioImpl: CopRepositoryImpl by lazy {
        // El token se lee del repositorio cada vez que se hace una petición
        lateinit var repo: CopRepositoryImpl
        val api = CopApi(ApiClient.httpClient, urlServidor()) { repo.token }
        repo = CopRepositoryImpl(api)
        repo
    }

    val repository: CopRepository by lazy { repositorioImpl }

    /** La navegación se suscribe para regresar al login si el token vence. */
    fun alExpirarSesion(accion: () -> Unit) {
        repositorioImpl.alExpirarSesion = accion
    }

    val iniciarSesion by lazy { IniciarSesionUseCase(repository) }
    val obtenerServicios by lazy { ObtenerServiciosUseCase(repository) }
    val obtenerProfesionales by lazy { ObtenerProfesionalesUseCase(repository) }
    val obtenerCitas by lazy { ObtenerCitasUseCase(repository) }
    val agendarCita by lazy { AgendarCitaUseCase(repository) }
    val registrarUsuario by lazy { RegistrarUsuarioUseCase(repository) }
    val poligonosCercanos by lazy { PoligonosCercanosUseCase() }
}
