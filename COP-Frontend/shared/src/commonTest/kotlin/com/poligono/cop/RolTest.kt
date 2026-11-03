package com.poligono.cop

import com.poligono.cop.composeApp.componentes.formatearFecha
import com.poligono.cop.composeApp.componentes.iniciales
import com.poligono.cop.composeApp.navegacion.Pantalla
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Usuario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Pruebas de la lógica que no depende de la red ni de la pantalla. */
class RolTest {

    @Test
    fun convierteTextoDelServidorEnRol() {
        assertEquals(Rol.PERSONAL, Rol.desdeTexto("PERSONAL"))
        assertEquals(Rol.PROFESIONAL, Rol.desdeTexto("profesional"))
    }

    @Test
    fun rolDesconocidoSeVuelveUsuario() {
        assertEquals(Rol.USUARIO, Rol.desdeTexto("HACKER"))
        assertEquals(Rol.USUARIO, Rol.desdeTexto(null))
    }

    @Test
    fun cadaRolVaASuPantalla() {
        val base = Usuario("x", "Prueba", Rol.USUARIO)
        assertIs<Pantalla.PanelAdministrador>(Pantalla.segunRol(base.copy(rol = Rol.ADMINISTRADOR)))
        assertIs<Pantalla.PanelPersonal>(Pantalla.segunRol(base.copy(rol = Rol.PERSONAL)))
        assertIs<Pantalla.PanelProfesional>(Pantalla.segunRol(base.copy(rol = Rol.PROFESIONAL)))
        assertIs<Pantalla.PanelUsuario>(Pantalla.segunRol(base))
    }

    @Test
    fun formatosDeApoyo() {
        assertEquals("12/10/2026", formatearFecha("2026-10-12"))
        assertEquals("AL", iniciales("Ana López"))
        assertEquals("CO", iniciales("Lic. Carlos Ortiz"))
    }
}
