package com.poligono.cop.rutas

import com.poligono.cop.consulta
import com.poligono.cop.data.CitaRepositorio
import com.poligono.cop.data.FiltroCitas
import com.poligono.cop.data.NotificacionRepositorio
import com.poligono.cop.idDeRuta
import com.poligono.cop.models.CambioEstadoRequest
import com.poligono.cop.models.CitaManualRequest
import com.poligono.cop.models.CitaRequest
import com.poligono.cop.models.ReprogramarRequest
import com.poligono.cop.models.RespuestaOk
import com.poligono.cop.models.Roles
import com.poligono.cop.requerirRol
import com.poligono.cop.services.ErrorNegocio
import com.poligono.cop.services.ReglasCitas
import com.poligono.cop.usuarioActual
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/** Citas (COP-RN-05 a 18), disponibilidad y avisos (COP-RN-11). Todas requieren sesión. */
fun Route.rutasCitas() {

    /**
     * GET /citas — confidencialidad (COP-RC-04):
     *  - USUARIO: solo sus citas, aunque mande otro ?paciente=
     *  - PROFESIONAL: solo las que atiende
     *  - PERSONAL y ADMINISTRADOR: agenda completa con filtros
     *    (?fecha=, ?desde=, ?hasta=, ?servicioId=, ?profesional=, ?paciente=, ?estado=)
     */
    get("/citas") {
        val usuario = call.usuarioActual()
        val fecha = call.consulta("fecha")

        val filtro = FiltroCitas(
            desde = fecha ?: call.consulta("desde"),
            hasta = fecha ?: call.consulta("hasta"),
            servicioId = call.consulta("servicioId"),
            profesional = call.consulta("profesional"),
            paciente = call.consulta("paciente"),
            estado = call.consulta("estado")
        )

        val permitido = when (usuario.rol) {
            Roles.USUARIO -> filtro.copy(paciente = usuario.username, profesional = null)
            Roles.PROFESIONAL -> filtro.copy(profesional = usuario.username)
            else -> filtro
        }

        call.respond(CitaRepositorio.listar(permitido))
    }

    // COP-RN-05: solicitar una cita
    post("/citas") {
        val usuario = call.usuarioActual()
        usuario.requerirRol(Roles.USUARIO)

        val p = call.receive<CitaRequest>()
        val cita = ReglasCitas.solicitar(usuario, p.profesional, p.fecha, p.hora, p.notas)

        call.respond(HttpStatusCode.Created, cita)
    }

    // COP-RN-15: cita presencial o telefónica
    post("/citas/manual") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<CitaManualRequest>()
        val cita = ReglasCitas.registrarManual(
            profesional = p.profesional,
            fecha = p.fecha,
            hora = p.hora,
            nombreContacto = p.nombreContacto,
            telefonoContacto = p.telefonoContacto,
            notas = p.notas
        )

        call.respond(HttpStatusCode.Created, cita)
    }

    // COP-RN-08: cancelar la propia cita
    put("/citas/{id}/cancelar") {
        val usuario = call.usuarioActual()
        usuario.requerirRol(Roles.USUARIO)

        call.respond(ReglasCitas.cancelarPorUsuario(usuario, call.idDeRuta()))
    }

    // COP-RN-09 (usuario) y COP-RN-16 (personal)
    put("/citas/{id}/reprogramar") {
        val usuario = call.usuarioActual()
        usuario.requerirRol(Roles.USUARIO, *Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<ReprogramarRequest>()

        call.respond(ReglasCitas.reprogramar(usuario, call.idDeRuta(), p.fecha, p.hora, p.motivo))
    }

    // COP-RN-12 confirmar/rechazar, COP-RN-16 cancelar, COP-RN-18 atendida/inasistencia
    put("/citas/{id}/estado") {
        val usuario = call.usuarioActual()
        usuario.requerirRol(Roles.PROFESIONAL, *Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<CambioEstadoRequest>()

        call.respond(ReglasCitas.cambiarEstado(usuario, call.idDeRuta(), p.estado, p.motivo))
    }

    // Horas libres de un profesional en una fecha
    get("/disponibilidad") {
        call.usuarioActual()

        val profesional = call.consulta("profesional")
            ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "Falta el profesional.")
        val fecha = call.consulta("fecha")
            ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "Falta la fecha.")
        val excluir = call.consulta("excluirCitaId")?.toIntOrNull()

        call.respond(ReglasCitas.horasLibres(profesional, fecha, excluir))
    }

    // Próximos días con lugares libres
    get("/disponibilidad/fechas") {
        call.usuarioActual()

        val profesional = call.consulta("profesional")
            ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "Falta el profesional.")

        call.respond(ReglasCitas.fechasDisponibles(profesional))
    }

    get("/notificaciones") {
        val usuario = call.usuarioActual()
        call.respond(NotificacionRepositorio.delUsuario(usuario.username))
    }

    put("/notificaciones/{id}/leida") {
        val usuario = call.usuarioActual()
        NotificacionRepositorio.marcarLeida(call.idDeRuta(), usuario.username)

        call.respond(RespuestaOk(mensaje = "Listo"))
    }

    put("/notificaciones/leidas") {
        val usuario = call.usuarioActual()
        NotificacionRepositorio.marcarTodasLeidas(usuario.username)

        call.respond(RespuestaOk(mensaje = "Listo"))
    }
}
