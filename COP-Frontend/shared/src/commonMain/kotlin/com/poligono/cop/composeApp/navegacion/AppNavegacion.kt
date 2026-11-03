package com.poligono.cop.composeApp.navegacion

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.pantallas.LoginScreen
import com.poligono.cop.composeApp.pantallas.PanelAdministradorScreen
import com.poligono.cop.composeApp.pantallas.PanelPersonalScreen
import com.poligono.cop.composeApp.pantallas.PanelProfesionalScreen
import com.poligono.cop.composeApp.pantallas.PanelUsuarioScreen
import com.poligono.cop.domain.biometria.AccesoBiometrico
import com.poligono.cop.ui.NavegacionViewModel

/**
 * Decide QUÉ pantalla dibujar según pantallaActual.
 * Cuando pantallaActual cambia, Compose vuelve a ejecutar esta función
 * ("recomposición") y se dibuja la pantalla nueva.
 */
@Composable
fun AppNavegacion(
    acceso: AccesoBiometrico,
    navegacion: NavegacionViewModel = viewModel { NavegacionViewModel() }
) {
    val cerrarSesion = { navegacion.cerrarSesion() }

    when (val pantalla = navegacion.pantallaActual) {
        Pantalla.Login -> LoginScreen(
            acceso = acceso,
            onLoginExitoso = { usuario -> navegacion.iniciarSesion(usuario) }
        )
        is Pantalla.PanelAdministrador -> PanelAdministradorScreen(pantalla.usuario, cerrarSesion)
        is Pantalla.PanelPersonal -> PanelPersonalScreen(pantalla.usuario, cerrarSesion)
        is Pantalla.PanelProfesional -> PanelProfesionalScreen(pantalla.usuario, cerrarSesion)
        is Pantalla.PanelUsuario -> PanelUsuarioScreen(pantalla.usuario, cerrarSesion)
    }
}
