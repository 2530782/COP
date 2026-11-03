package com.poligono.cop.composeApp.navegacion

import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Usuario

/**
 * Todas las pantallas (rutas) de la app.
 * "sealed" = la lista es CERRADA: el compilador conoce todas las opciones,
 * así en un "when" no se nos puede olvidar ninguna.
 */
sealed interface Pantalla {
    data object Login : Pantalla
    data class PanelAdministrador(val usuario: Usuario) : Pantalla
    data class PanelPersonal(val usuario: Usuario) : Pantalla
    data class PanelProfesional(val usuario: Usuario) : Pantalla
    data class PanelUsuario(val usuario: Usuario) : Pantalla

    companion object {
        /** AQUÍ SE ENRUTA POR ROL: cada rol tiene su propia vista. */
        fun segunRol(usuario: Usuario): Pantalla = when (usuario.rol) {
            Rol.ADMINISTRADOR -> PanelAdministrador(usuario)
            Rol.PERSONAL -> PanelPersonal(usuario)
            Rol.PROFESIONAL -> PanelProfesional(usuario)
            Rol.USUARIO -> PanelUsuario(usuario)
        }
    }
}
