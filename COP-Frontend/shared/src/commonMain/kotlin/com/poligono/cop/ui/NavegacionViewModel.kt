package com.poligono.cop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.domain.model.Usuario

/**
 * Guarda en qué pantalla está la app. Al estar en un ViewModel,
 * si giras el teléfono NO te regresa al login.
 */
class NavegacionViewModel : ViewModel() {

    var pantallaActual by mutableStateOf<Pantalla>(Pantalla.Login)
        private set

    /** Después del login: enruta según el rol. */
    fun iniciarSesion(usuario: Usuario) {
        pantallaActual = Pantalla.segunRol(usuario)
    }

    fun cerrarSesion() {
        pantallaActual = Pantalla.Login
    }
}
