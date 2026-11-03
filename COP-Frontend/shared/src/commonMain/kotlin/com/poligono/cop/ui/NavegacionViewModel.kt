package com.poligono.cop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.data.local.Avisos
import com.poligono.cop.di.Dependencias
import com.poligono.cop.domain.model.Configuracion
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.domain.repository.CopRepository
import com.poligono.cop.plataforma.ahoraMillis
import com.poligono.cop.plataforma.cancelarRevisionAvisos
import com.poligono.cop.plataforma.programarRevisionAvisos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Guarda en qué pantalla está la app. Al estar en un ViewModel,
 * si giras el teléfono NO te regresa al login.
 *
 * Ahora es una PILA de pantallas: ir() agrega una, atras() quita la última.
 * También se encarga de lo que dura toda la sesión:
 *  - COP-RN-02: si hay una sesión guardada, entra directo a la vista del rol
 *  - COP-RNN-08: cierra la sesión tras unos minutos sin usar la app
 *  - COP-RC-02 / RN-11: cada 30 s revisa avisos y cambios de citas (rol USUARIO)
 */
class NavegacionViewModel(
    private val repository: CopRepository = Dependencias.repository
) : ViewModel() {

    private val pila = mutableStateListOf<Pantalla>()

    val pantallaActual: Pantalla
        get() = pila.lastOrNull() ?: Pantalla.Login

    val puedeRegresar: Boolean
        get() = pila.size > 1

    /** true si la primera pantalla de la pila es un panel (hay sesión iniciada). */
    val haySesion: Boolean
        get() = pila.firstOrNull().let { it != null && it != Pantalla.Login }

    /** Mensaje para mostrar en el login (ej. "se cerró por inactividad"). */
    var mensajeLogin by mutableStateOf<String?>(null)
        private set

    var avisosSinLeer by mutableStateOf(0)
        private set

    var configuracion by mutableStateOf(Configuracion())
        private set

    private var ultimaActividad = ahoraMillis()
    private var tareasDeSesion: Job? = null

    init {
        val guardado = repository.sesionGuardada()
        pila.add(if (guardado != null) Pantalla.segunRol(guardado) else Pantalla.Login)

        Dependencias.alExpirarSesion {
            cerrarSesion("Tu sesión expiró. Inicia sesión de nuevo.")
        }

        viewModelScope.launch {
            repository.obtenerConfiguracion().onSuccess { configuracion = it }
        }

        if (guardado != null) {
            iniciarTareasDeSesion(guardado)
        }
    }

    /** Después del login: enruta según el rol. */
    fun iniciarSesion(usuario: Usuario) {
        mensajeLogin = null
        pila.clear()
        pila.add(Pantalla.segunRol(usuario))
        registrarActividad()
        iniciarTareasDeSesion(usuario)
    }

    fun cerrarSesion(mensaje: String? = null) {
        tareasDeSesion?.cancel()
        repository.cerrarSesion()
        cancelarRevisionAvisos()
        avisosSinLeer = 0
        mensajeLogin = mensaje
        pila.clear()
        pila.add(Pantalla.Login)
    }

    fun ir(pantalla: Pantalla) {
        pila.add(pantalla)
    }

    fun atras() {
        if (pila.size > 1) {
            pila.removeAt(pila.lastIndex)
        }
    }

    /** Regresa a la pantalla de inicio del rol y abre [pantalla] encima. */
    fun irDesdeInicio(pantalla: Pantalla) {
        val inicio = pila.firstOrNull() ?: Pantalla.Login
        pila.clear()
        pila.add(inicio)
        pila.add(pantalla)
    }

    /** Cada toque en la pantalla cuenta como actividad. */
    fun registrarActividad() {
        ultimaActividad = ahoraMillis()
    }

    fun actualizarAvisosSinLeer(cantidad: Int) {
        avisosSinLeer = cantidad
    }

    private fun iniciarTareasDeSesion(usuario: Usuario) {
        tareasDeSesion?.cancel()

        if (usuario.rol == Rol.USUARIO) {
            programarRevisionAvisos()
        }

        tareasDeSesion = viewModelScope.launch {
            // COP-RNN-08: cierre por inactividad
            launch {
                while (true) {
                    delay(15_000)
                    val limite = configuracion.minutosInactividad * 60_000L

                    if (ahoraMillis() - ultimaActividad > limite) {
                        cerrarSesion("Tu sesión se cerró por inactividad. Vuelve a iniciar sesión.")
                    }
                }
            }

            // COP-RC-02: sincronización cada 30 segundos (solo pacientes)
            if (usuario.rol == Rol.USUARIO) {
                launch {
                    while (true) {
                        repository.obtenerNotificaciones().onSuccess { avisos ->
                            Avisos.mostrarNuevos(avisos)
                            avisosSinLeer = avisos.count { !it.leida }
                        }

                        repository.obtenerCitas(paciente = usuario.username).onSuccess { citas ->
                            Avisos.sincronizarRecordatorios(citas)
                        }

                        delay(30_000)
                    }
                }
            }
        }
    }
}
