package com.poligono.cop.composeApp.navegacion

import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.plataforma.ahoraMillis

/**
 * Todas las pantallas (rutas) de la app.
 * "sealed" = la lista es CERRADA: el compilador conoce todas las opciones,
 * así en un "when" no se nos puede olvidar ninguna.
 *
 * [visita] hace que cada vez que se abre una pantalla de formulario se cree un
 * ViewModel nuevo (si no, al volver a "Agendar" aparecería la cita anterior).
 */
sealed interface Pantalla {
    // ---------- Sin sesión ----------
    data object Login : Pantalla
    data object Registro : Pantalla
    data object Recuperar : Pantalla
    data object Poligonos : Pantalla
    data object Ayuda : Pantalla

    // ---------- Vista principal de cada rol ----------
    data class PanelAdministrador(val usuario: Usuario) : Pantalla
    data class PanelPersonal(val usuario: Usuario) : Pantalla
    data class PanelProfesional(val usuario: Usuario) : Pantalla
    data class PanelUsuario(val usuario: Usuario) : Pantalla

    // ---------- Pantallas que se abren desde un panel ----------
    /** Agendar (USUARIO) o registrar cita presencial/telefónica ([manual], PERSONAL). */
    data class Agendar(
        val usuario: Usuario,
        val manual: Boolean = false,
        val servicioId: String? = null,
        val visita: Long = ahoraMillis()
    ) : Pantalla

    data class Reprogramar(
        val usuario: Usuario,
        val cita: Cita,
        val visita: Long = ahoraMillis()
    ) : Pantalla

    data class Notificaciones(val usuario: Usuario) : Pantalla

    /** Agenda del Centro abierta desde el panel del administrador. */
    data class Agenda(val usuario: Usuario) : Pantalla

    data class Catalogo(val usuario: Usuario, val visita: Long = ahoraMillis()) : Pantalla
    data class Horarios(val usuario: Usuario, val visita: Long = ahoraMillis()) : Pantalla
    data class Exportar(val usuario: Usuario) : Pantalla
    data class Reportes(val usuario: Usuario, val visita: Long = ahoraMillis()) : Pantalla

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
