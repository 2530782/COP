package com.poligono.cop.composeApp.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poligono.cop.composeApp.pantallas.AgendarScreen
import com.poligono.cop.composeApp.pantallas.AyudaScreen
import com.poligono.cop.composeApp.pantallas.CatalogoScreen
import com.poligono.cop.composeApp.pantallas.ExportarScreen
import com.poligono.cop.composeApp.pantallas.HorariosScreen
import com.poligono.cop.composeApp.pantallas.LoginScreen
import com.poligono.cop.composeApp.pantallas.NotificacionesScreen
import com.poligono.cop.composeApp.pantallas.PanelAdministradorScreen
import com.poligono.cop.composeApp.pantallas.PanelPersonalScreen
import com.poligono.cop.composeApp.pantallas.PanelProfesionalScreen
import com.poligono.cop.composeApp.pantallas.PanelUsuarioScreen
import com.poligono.cop.composeApp.pantallas.PoligonosScreen
import com.poligono.cop.composeApp.pantallas.RecuperarScreen
import com.poligono.cop.composeApp.pantallas.RegistroScreen
import com.poligono.cop.composeApp.pantallas.ReportesScreen
import com.poligono.cop.composeApp.pantallas.ReprogramarScreen
import com.poligono.cop.domain.biometria.AccesoBiometrico
import com.poligono.cop.plataforma.ManejarAtras
import com.poligono.cop.plataforma.PedirPermisoNotificaciones
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
    val regresar = { navegacion.atras() }
    val ir = { pantalla: Pantalla -> navegacion.ir(pantalla) }
    val pantalla = navegacion.pantallaActual

    // Botón "atrás" del teléfono: regresa a la pantalla anterior
    ManejarAtras(habilitado = navegacion.puedeRegresar) {
        navegacion.atras()
    }

    // Los pacientes reciben recordatorios: se pide permiso de notificaciones (Android 13+)
    if (pantalla is Pantalla.PanelUsuario) {
        PedirPermisoNotificaciones()
    }

    // Cualquier toque cuenta como actividad (COP-RNN-08). No se "consume" el toque.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        navegacion.registrarActividad()
                    }
                }
            }
    ) {
        when (pantalla) {
            Pantalla.Login -> LoginScreen(
                acceso = acceso,
                mensajeInicial = navegacion.mensajeLogin,
                onLoginExitoso = { usuario -> navegacion.iniciarSesion(usuario) },
                onRegistrarse = { ir(Pantalla.Registro) },
                onRecuperar = { ir(Pantalla.Recuperar) },
                onVerPoligonos = { ir(Pantalla.Poligonos) },
                onAyuda = { ir(Pantalla.Ayuda) }
            )

            Pantalla.Registro -> RegistroScreen(
                onRegistrado = { usuario -> navegacion.iniciarSesion(usuario) },
                onRegresar = regresar
            )

            Pantalla.Recuperar -> RecuperarScreen(onRegresar = regresar)

            Pantalla.Poligonos -> PoligonosScreen(onRegresar = regresar)

            Pantalla.Ayuda -> AyudaScreen(
                configuracion = navegacion.configuracion,
                conSesion = navegacion.haySesion,
                onRegresar = regresar
            )

            is Pantalla.PanelAdministrador -> PanelAdministradorScreen(pantalla.usuario, cerrarSesion, ir)

            is Pantalla.PanelPersonal -> PanelPersonalScreen(pantalla.usuario, cerrarSesion, ir)

            is Pantalla.PanelProfesional -> PanelProfesionalScreen(pantalla.usuario, cerrarSesion, ir)

            is Pantalla.PanelUsuario -> PanelUsuarioScreen(
                usuario = pantalla.usuario,
                configuracion = navegacion.configuracion,
                avisosSinLeer = navegacion.avisosSinLeer,
                onCerrarSesion = cerrarSesion,
                onNavegar = ir
            )

            is Pantalla.Agendar -> AgendarScreen(
                pantalla = pantalla,
                onTerminar = regresar,
                onRegresar = regresar
            )

            is Pantalla.Reprogramar -> ReprogramarScreen(
                pantalla = pantalla,
                onTerminar = regresar,
                onRegresar = regresar
            )

            is Pantalla.Notificaciones -> NotificacionesScreen(
                onCambioSinLeer = { navegacion.actualizarAvisosSinLeer(it) },
                onRegresar = regresar
            )

            is Pantalla.Agenda -> PanelPersonalScreen(pantalla.usuario, cerrarSesion, ir, onRegresar = regresar)

            is Pantalla.Catalogo -> CatalogoScreen(pantalla, onRegresar = regresar)

            is Pantalla.Horarios -> HorariosScreen(pantalla, onRegresar = regresar)

            is Pantalla.Exportar -> ExportarScreen(onRegresar = regresar)

            is Pantalla.Reportes -> ReportesScreen(pantalla, onRegresar = regresar)
        }
    }
}
