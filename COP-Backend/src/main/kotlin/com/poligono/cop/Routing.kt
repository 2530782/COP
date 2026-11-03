package com.poligono.cop

import com.poligono.cop.rutas.rutasAuth
import com.poligono.cop.rutas.rutasCatalogo
import com.poligono.cop.rutas.rutasCatalogoAdmin
import com.poligono.cop.rutas.rutasCitas
import com.poligono.cop.rutas.rutasExportar
import com.poligono.cop.rutas.rutasPublicas
import com.poligono.cop.services.AuthService
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * RUTAS (endpoints) del backend COP. Cada grupo está en su archivo de la carpeta rutas/.
 *
 * Sin sesión:
 *   GET  /                      prueba rápida de que el servidor está vivo
 *   POST /login                 inicia sesión (regresa usuario y token)
 *   POST /registro              crea una cuenta de usuario
 *   POST /recuperar, /restablecer   recuperar contraseña
 *   GET  /configuracion         plazos y contacto del Centro
 *   GET  /poligonos             polígonos de Tulancingo para el mapa (GPS)
 *   GET  /servicios             catálogo de servicios
 *   GET  /profesionales         profesionales (?servicioId=FIS)
 *   GET  /exportar/agenda.pdf|csv   agenda (token en ?token=)
 *
 * Con sesión (Authorization: Bearer <token>):
 *   GET  /citas                 citas según el rol (el usuario solo ve las suyas)
 *   POST /citas                 solicitar cita       POST /citas/manual   cita presencial
 *   PUT  /citas/{id}/cancelar   PUT /citas/{id}/reprogramar   PUT /citas/{id}/estado
 *   GET  /disponibilidad, /disponibilidad/fechas
 *   GET  /notificaciones        PUT /notificaciones/{id}/leida   PUT /notificaciones/leidas
 *   Administración: /servicios, /profesionales, /horarios, /poligonos, /reportes
 */
fun Application.configureRouting(auth: AuthService) {
    routing {

        get("/") {
            call.respondText("Servidor COP funcionando")
        }

        rutasAuth(auth)
        rutasPublicas()
        rutasExportar()

        // El token es opcional: si viene, el administrador puede ver también lo inactivo
        authenticate(SESION, optional = true) {
            rutasCatalogo()
        }

        authenticate(SESION) {
            rutasCitas()
            rutasCatalogoAdmin()
        }
    }
}
