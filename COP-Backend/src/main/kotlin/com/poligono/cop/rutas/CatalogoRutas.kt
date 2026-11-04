package com.poligono.cop.rutas

import com.poligono.cop.Configuracion
import com.poligono.cop.consulta
import com.poligono.cop.data.Bitacora
import com.poligono.cop.data.HorarioRepositorio
import com.poligono.cop.data.PoligonoRepositorio
import com.poligono.cop.data.ReporteRepositorio
import com.poligono.cop.data.ServicioRepositorio
import com.poligono.cop.data.UsuarioRepositorio
import com.poligono.cop.idDeRuta
import com.poligono.cop.models.ConfiguracionDto
import com.poligono.cop.models.HorarioRequest
import com.poligono.cop.models.PoligonoRequest
import com.poligono.cop.models.ProfesionalRequest
import com.poligono.cop.models.ReporteRequest
import com.poligono.cop.models.RespuestaOk
import com.poligono.cop.models.Roles
import com.poligono.cop.models.Servicio
import com.poligono.cop.parametro
import com.poligono.cop.requerirRol
import com.poligono.cop.seguridad.Contrasenas
import com.poligono.cop.services.ErrorNegocio
import com.poligono.cop.services.Fechas
import com.poligono.cop.services.ReglasCitas
import com.poligono.cop.services.Validaciones
import com.poligono.cop.usuarioActual
import com.poligono.cop.usuarioOpcional
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/** Rutas sin sesión. */
fun Route.rutasPublicas() {

    get("/configuracion") {
        call.respond(
            ConfiguracionDto(
                plazoHorasCambio = Configuracion.plazoHorasCambio,
                horasRecordatorio = Configuracion.HORAS_RECORDATORIO,
                minutosInactividad = Configuracion.minutosInactividad,
                telefonoCentro = Configuracion.telefonoCentro,
                whatsappCentro = Configuracion.whatsappCentro,
                fechaHoy = Fechas.hoy().toString()
            )
        )
    }

    // Polígonos para el mapa con GPS (se pueden ver antes de iniciar sesión)
    get("/poligonos") {
        call.respond(PoligonoRepositorio.listar())
    }
}

/** Catálogo (COP-RN-03 y 04). Token opcional. */
fun Route.rutasCatalogo() {

    // Igual que antes: todos los servicios con su campo "activo"
    get("/servicios") {
        call.respond(ServicioRepositorio.listar())
    }

    // Profesionales activos (?servicioId=FIS). El personal con ?todos=true ve también los inactivos.
    get("/profesionales") {
        val usuario = call.usuarioOpcional()
        val todos = call.consulta("todos") == "true" && usuario != null && usuario.rol in Roles.PERSONAL_DEL_CENTRO

        call.respond(UsuarioRepositorio.profesionales(call.consulta("servicioId"), soloActivos = !todos))
    }
}

/** Administración del Centro (COP-RN-14 y 17, COP-RE-01) y reportes (COP-RS-07). Requiere sesión. */
fun Route.rutasCatalogoAdmin() {

    get("/profesionales/{username}/horarios") {
        call.usuarioActual()
        call.respond(HorarioRepositorio.delProfesional(call.parametro("username")))
    }

    post("/servicios") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val s = call.receive<Servicio>()
        validarServicio(s)

        if (ServicioRepositorio.buscar(s.id.trim().uppercase()) != null) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ya existe un servicio con la clave ${s.id}.")
        }

        ServicioRepositorio.crear(s)
        call.respond(HttpStatusCode.Created, ServicioRepositorio.listar())
    }

    put("/servicios/{id}") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val id = call.parametro("id")
        val s = call.receive<Servicio>().copy(id = id)
        validarServicio(s)

        ServicioRepositorio.buscar(id)
            ?: throw ErrorNegocio(HttpStatusCode.NotFound, "El servicio no existe.")

        ServicioRepositorio.actualizar(s)
        call.respond(ServicioRepositorio.listar())
    }

    // Un profesional es un usuario con rol PROFESIONAL (puede iniciar sesión y ver su agenda)
    post("/profesionales") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<ProfesionalRequest>()
        Validaciones.username(p.username)
        Validaciones.nombre(p.nombre)
        Validaciones.contrasena(p.password)

        ServicioRepositorio.buscar(p.servicioId)
            ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "El servicio elegido no existe.")

        if (UsuarioRepositorio.buscar(p.username) != null) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ese nombre de usuario ya existe.")
        }

        UsuarioRepositorio.crear(
            username = p.username,
            contrasenaHash = Contrasenas.cifrar(p.password),
            nombre = p.nombre,
            rol = Roles.PROFESIONAL,
            servicioId = p.servicioId,
            telefono = "",
            correo = "",
            aceptoPrivacidad = true
        )

        Bitacora.info("Profesional ${p.username} dado de alta")
        call.respond(HttpStatusCode.Created, UsuarioRepositorio.profesionales(null, soloActivos = false))
    }

    put("/profesionales/{username}") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val username = call.parametro("username")
        val p = call.receive<ProfesionalRequest>()
        Validaciones.nombre(p.nombre)

        val anterior = UsuarioRepositorio.buscar(username)

        if (anterior == null || anterior.rol != Roles.PROFESIONAL) {
            throw ErrorNegocio(HttpStatusCode.NotFound, "El profesional no existe.")
        }

        ServicioRepositorio.buscar(p.servicioId)
            ?: throw ErrorNegocio(HttpStatusCode.BadRequest, "El servicio elegido no existe.")

        UsuarioRepositorio.actualizarProfesional(username, p.nombre, p.servicioId, p.activo)

        if (p.password.isNotBlank()) {
            Validaciones.contrasena(p.password)
            UsuarioRepositorio.cambiarContrasena(username, Contrasenas.cifrar(p.password))
        }

        // COP-RNN-05: si se desactiva, se avisa a los usuarios con citas futuras
        if (anterior.activo && !p.activo) {
            ReglasCitas.profesionalNoDisponible(username)
        }

        call.respond(UsuarioRepositorio.profesionales(null, soloActivos = false))
    }

    post("/horarios") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<HorarioRequest>()
        Validaciones.horario(p.diaSemana, p.horaInicio, p.horaFin)

        val profesional = UsuarioRepositorio.buscar(p.profesional)

        if (profesional == null || profesional.rol != Roles.PROFESIONAL) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "El profesional no existe.")
        }

        val traslape = HorarioRepositorio.delProfesional(profesional.username).any {
            it.diaSemana == p.diaSemana && p.horaInicio < it.horaFin && it.horaInicio < p.horaFin
        }

        if (traslape) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ese horario se encima con otro del mismo día.")
        }

        HorarioRepositorio.crear(profesional.username, p.diaSemana, p.horaInicio, p.horaFin)
        call.respond(HttpStatusCode.Created, HorarioRepositorio.delProfesional(profesional.username))
    }

    delete("/horarios/{id}") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        if (HorarioRepositorio.eliminar(call.idDeRuta()) == 0) {
            throw ErrorNegocio(HttpStatusCode.NotFound, "El horario no existe.")
        }

        call.respond(RespuestaOk(mensaje = "Horario eliminado. Las citas ya agendadas se conservan."))
    }

    // El personal puede corregir la ubicación de los polígonos sin cambiar la app (COP-RE-01)
    post("/poligonos") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<PoligonoRequest>()
        Validaciones.texto(p.nombre, "nombre", 120)

        PoligonoRepositorio.crear(p)
        call.respond(HttpStatusCode.Created, PoligonoRepositorio.listar())
    }

    put("/poligonos/{id}") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)

        val p = call.receive<PoligonoRequest>()
        Validaciones.texto(p.nombre, "nombre", 120)

        PoligonoRepositorio.actualizar(call.idDeRuta(), p)
        call.respond(PoligonoRepositorio.listar())
    }

    post("/reportes") {
        val usuario = call.usuarioActual()
        val p = call.receive<ReporteRequest>()
        Validaciones.texto(p.descripcion, "descripción", 1000)

        ReporteRepositorio.crear(usuario.username, p.descripcion, p.dispositivo)
        Bitacora.info("Reporte de falla recibido de ${usuario.username}")

        call.respond(HttpStatusCode.Created, RespuestaOk(mensaje = "Gracias, recibimos tu reporte."))
    }

    get("/reportes") {
        call.usuarioActual().requerirRol(*Roles.PERSONAL_DEL_CENTRO)
        call.respond(ReporteRepositorio.listar())
    }
}

private fun validarServicio(s: Servicio) {
    Validaciones.texto(s.id, "clave", 10)
    Validaciones.texto(s.nombre, "nombre", 80)
    Validaciones.texto(s.descripcion, "descripción", 500)

    if (s.duracionMinutos !in 10..240) {
        throw ErrorNegocio(HttpStatusCode.BadRequest, "La duración debe ser de 10 a 240 minutos.")
    }
}
