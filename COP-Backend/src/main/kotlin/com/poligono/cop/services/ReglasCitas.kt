package com.poligono.cop.services

import com.poligono.cop.Configuracion
import com.poligono.cop.data.Bitacora
import com.poligono.cop.data.CitaRepositorio
import com.poligono.cop.data.FiltroCitas
import com.poligono.cop.data.HorarioRepositorio
import com.poligono.cop.data.NotificacionRepositorio
import com.poligono.cop.data.ServicioRepositorio
import com.poligono.cop.data.UsuarioFila
import com.poligono.cop.data.UsuarioRepositorio
import com.poligono.cop.models.CitaDto
import com.poligono.cop.models.EstadosCita
import com.poligono.cop.models.FechaDisponibleDto
import com.poligono.cop.models.Roles
import com.poligono.cop.models.UsuarioDto
import io.ktor.http.HttpStatusCode
import java.time.LocalDateTime

/**
 * Reglas de negocio de las citas:
 * COP-RN-05, 08, 09, 12, 15, 16, 18 y COP-RNN-01, 03, 04, 05.
 *
 * Los horarios se parten en espacios del tamaño del servicio
 * (duracionMinutos de servicios.json: fisioterapia 50 min, nutrición 40 min...).
 */
object ReglasCitas {

    // Evita que dos personas tomen el mismo horario al mismo tiempo (COP-RNN-01)
    private val candado = Any()

    private const val DIAS_A_MOSTRAR = 30
    private const val DIAS_MAXIMOS = 90L

    /** El profesional debe existir, estar activo y tener un servicio. */
    private fun profesionalValido(username: String): UsuarioFila {
        val profesional = UsuarioRepositorio.buscar(username)

        if (profesional == null || profesional.rol != Roles.PROFESIONAL || profesional.servicioId == null) {
            throw ErrorNegocio(HttpStatusCode.NotFound, "El profesional no existe.")
        }

        if (!profesional.activo) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Este profesional no está disponible por ahora.")
        }

        return profesional
    }

    /** Horas libres de un profesional en una fecha (COP-RN-04 y COP-RNN-03). */
    fun horasLibres(
        profesional: String,
        fecha: String,
        excluirCitaId: Int? = null
    ): List<String> {
        val fila = UsuarioRepositorio.buscar(profesional) ?: return emptyList()

        if (!fila.activo || fila.servicioId == null) {
            return emptyList()
        }

        val servicio = ServicioRepositorio.buscar(fila.servicioId) ?: return emptyList()

        if (!servicio.activo || servicio.duracionMinutos <= 0) {
            return emptyList()
        }

        val dia = Fechas.fecha(fecha)
        val ahora = Fechas.ahora()
        val duracion = servicio.duracionMinutos.toLong()
        val ocupadas = CitaRepositorio.horasOcupadas(fila.username, fecha, excluirCitaId)
        val horarios = HorarioRepositorio
            .delProfesional(fila.username)
            .filter { it.diaSemana == dia.dayOfWeek.value }

        val libres = mutableListOf<String>()

        for (horario in horarios) {
            var inicio = Fechas.hora(horario.horaInicio)
            val fin = Fechas.hora(horario.horaFin)

            while (!inicio.plusMinutes(duracion).isAfter(fin)) {
                val texto = inicio.format(Fechas.formatoHora)
                val esFuturo = LocalDateTime.of(dia, inicio).isAfter(ahora)

                if (esFuturo && texto !in ocupadas) {
                    libres.add(texto)
                }

                val siguiente = inicio.plusMinutes(duracion)

                // Si el horario cruza la medianoche, se detiene
                if (siguiente.isBefore(inicio)) {
                    break
                }

                inicio = siguiente
            }
        }

        return libres.distinct().sorted()
    }

    /** Próximos días con lugares libres (para elegir en pocos pasos, COP-RC-06). */
    fun fechasDisponibles(profesional: String): List<FechaDisponibleDto> {
        val hoy = Fechas.hoy()
        val resultado = mutableListOf<FechaDisponibleDto>()

        for (i in 0 until DIAS_A_MOSTRAR) {
            val fecha = hoy.plusDays(i.toLong()).toString()
            val libres = horasLibres(profesional, fecha).size

            if (libres > 0) {
                resultado.add(FechaDisponibleDto(fecha, libres))
            }
        }

        return resultado
    }

    /** COP-RN-05: el usuario solicita una cita; queda PENDIENTE hasta que el personal la confirme. */
    fun solicitar(
        usuario: UsuarioDto,
        profesional: String,
        fecha: String,
        hora: String,
        notas: String
    ): CitaDto {
        val id = registrar(
            paciente = usuario.username,
            nombreContacto = null,
            telefonoContacto = null,
            profesional = profesional,
            fecha = fecha,
            hora = hora,
            estado = EstadosCita.PENDIENTE,
            notas = notas,
            creadoPor = Roles.USUARIO
        )

        Bitacora.info("Cita $id solicitada por ${usuario.username}")
        return CitaRepositorio.buscar(id)!!
    }

    /** COP-RN-15: el personal registra una cita presencial o telefónica (queda confirmada). */
    fun registrarManual(
        profesional: String,
        fecha: String,
        hora: String,
        nombreContacto: String,
        telefonoContacto: String,
        notas: String
    ): CitaDto {
        Validaciones.nombre(nombreContacto)
        Validaciones.telefono(telefonoContacto)

        val id = registrar(
            paciente = null,
            nombreContacto = nombreContacto.trim(),
            telefonoContacto = telefonoContacto.filter { it.isDigit() },
            profesional = profesional,
            fecha = fecha,
            hora = hora,
            estado = EstadosCita.CONFIRMADA,
            notas = notas,
            creadoPor = Roles.PERSONAL
        )

        Bitacora.info("Cita manual $id registrada por el personal")
        return CitaRepositorio.buscar(id)!!
    }

    private fun registrar(
        paciente: String?,
        nombreContacto: String?,
        telefonoContacto: String?,
        profesional: String,
        fecha: String,
        hora: String,
        estado: String,
        notas: String,
        creadoPor: String
    ): Int {
        val fila = profesionalValido(profesional)

        if (notas.length > 500) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "Las notas no deben pasar de 500 caracteres.")
        }

        synchronized(candado) {
            validarHorarioLibre(fila.username, fecha, hora, null)

            return CitaRepositorio.crear(
                servicioId = fila.servicioId!!,
                profesional = fila.username,
                paciente = paciente,
                nombreContacto = nombreContacto,
                telefonoContacto = telefonoContacto,
                fecha = fecha,
                hora = hora,
                estado = estado,
                notas = notas,
                creadoPor = creadoPor
            )
        }
    }

    /** COP-RN-08 y COP-RNN-04: cancelar la propia cita dentro del plazo. */
    fun cancelarPorUsuario(
        usuario: UsuarioDto,
        citaId: Int
    ): CitaDto {
        val cita = citaDelUsuario(usuario, citaId)
        validarPlazo(cita, "cancelar")

        CitaRepositorio.cambiarEstado(citaId, EstadosCita.CANCELADA, "Cancelada por el usuario")
        Bitacora.info("Cita $citaId cancelada por ${usuario.username}")

        return CitaRepositorio.buscar(citaId)!!
    }

    /**
     * COP-RN-09 (usuario, dentro del plazo; vuelve a quedar pendiente) y
     * COP-RN-16 (personal, sin plazo; queda confirmada y se avisa al usuario).
     */
    fun reprogramar(
        actor: UsuarioDto,
        citaId: Int,
        fecha: String,
        hora: String,
        motivo: String
    ): CitaDto {
        val esPersonal = actor.rol in Roles.PERSONAL_DEL_CENTRO

        val cita = if (esPersonal) {
            CitaRepositorio.buscar(citaId) ?: throw ErrorNegocio(HttpStatusCode.NotFound, "La cita no existe.")
        } else {
            citaDelUsuario(actor, citaId)
        }

        if (esPersonal) {
            if (cita.estado !in EstadosCita.ACTIVOS) {
                throw ErrorNegocio(HttpStatusCode.Conflict, "Solo se pueden modificar citas pendientes o confirmadas.")
            }
        } else {
            validarPlazo(cita, "reprogramar")
        }

        val nuevoEstado = if (esPersonal) EstadosCita.CONFIRMADA else EstadosCita.PENDIENTE
        val textoMotivo = if (esPersonal) motivo else "Reprogramada por el usuario"

        synchronized(candado) {
            validarHorarioLibre(cita.profesional, fecha, hora, citaId)
            CitaRepositorio.cambiarFechaHora(citaId, fecha, hora, nuevoEstado, textoMotivo)
        }

        if (esPersonal) {
            avisar(
                cita = cita,
                titulo = "Tu cita fue modificada",
                mensaje = "Tu cita de ${cita.servicio} ahora es el ${Fechas.legible(fecha, hora)}." + textoMotivo(motivo)
            )
        }

        Bitacora.info("Cita $citaId reprogramada por ${actor.username}")
        return CitaRepositorio.buscar(citaId)!!
    }

    /**
     * COP-RN-12 (confirmar o rechazar), COP-RN-16 (cancelar) y COP-RN-18 (atendida o inasistencia).
     * El profesional solo puede marcar atendida o inasistencia en sus propias citas.
     */
    fun cambiarEstado(
        actor: UsuarioDto,
        citaId: Int,
        nuevoEstado: String,
        motivo: String
    ): CitaDto {
        val cita = CitaRepositorio.buscar(citaId)
            ?: throw ErrorNegocio(HttpStatusCode.NotFound, "La cita no existe.")

        val estado = nuevoEstado.uppercase()

        if (actor.rol == Roles.PROFESIONAL) {
            if (cita.profesional != actor.username) {
                throw ErrorNegocio(HttpStatusCode.Forbidden, "Esta cita no pertenece a tu agenda.")
            }

            if (estado != EstadosCita.ATENDIDA && estado != EstadosCita.INASISTENCIA) {
                throw ErrorNegocio(HttpStatusCode.Forbidden, "Solo puedes marcar la cita como atendida o inasistencia.")
            }
        }

        when (estado) {
            EstadosCita.CONFIRMADA,
            EstadosCita.RECHAZADA -> {
                if (cita.estado != EstadosCita.PENDIENTE) {
                    throw ErrorNegocio(HttpStatusCode.Conflict, "Solo se pueden confirmar o rechazar solicitudes pendientes.")
                }
            }

            EstadosCita.CANCELADA -> {
                if (cita.estado !in EstadosCita.ACTIVOS) {
                    throw ErrorNegocio(HttpStatusCode.Conflict, "La cita ya no está activa.")
                }
            }

            EstadosCita.ATENDIDA,
            EstadosCita.INASISTENCIA -> {
                if (cita.estado !in EstadosCita.ACTIVOS) {
                    throw ErrorNegocio(HttpStatusCode.Conflict, "La cita ya no está activa.")
                }

                if (Fechas.fecha(cita.fecha).isAfter(Fechas.hoy())) {
                    throw ErrorNegocio(HttpStatusCode.Conflict, "No se puede marcar una cita de un día que todavía no llega.")
                }
            }

            else -> throw ErrorNegocio(HttpStatusCode.BadRequest, "Estado no válido: $nuevoEstado")
        }

        CitaRepositorio.cambiarEstado(citaId, estado, motivo)
        Bitacora.info("Cita $citaId cambió a $estado por ${actor.username}")

        when (estado) {
            EstadosCita.CONFIRMADA -> avisar(
                cita = cita,
                titulo = "Cita confirmada",
                mensaje = "Tu cita de ${cita.servicio} del ${Fechas.legible(cita.fecha, cita.hora)} está confirmada."
            )

            EstadosCita.RECHAZADA -> avisar(
                cita = cita,
                titulo = "Solicitud no aceptada",
                mensaje = "Tu solicitud de ${cita.servicio} del ${Fechas.legible(cita.fecha, cita.hora)} no pudo aceptarse." +
                    textoMotivo(motivo) + " Puedes elegir otro horario en la app."
            )

            EstadosCita.CANCELADA -> avisar(
                cita = cita,
                titulo = "Cita cancelada por el Centro",
                mensaje = "Tu cita de ${cita.servicio} del ${Fechas.legible(cita.fecha, cita.hora)} fue cancelada." +
                    textoMotivo(motivo) + " Puedes agendar otra en la app."
            )
        }

        return CitaRepositorio.buscar(citaId)!!
    }

    /**
     * COP-RNN-05: si un profesional deja de estar disponible se cancelan sus citas futuras
     * y se avisa a cada usuario para que reprograme con otro profesional u horario.
     */
    fun profesionalNoDisponible(profesional: String): Int {
        val afectadas = CitaRepositorio.listar(
            FiltroCitas(
                desde = Fechas.hoy().toString(),
                profesional = profesional
            )
        ).filter { it.estado in EstadosCita.ACTIVOS }

        for (cita in afectadas) {
            CitaRepositorio.cambiarEstado(cita.id, EstadosCita.CANCELADA, "El profesional ya no está disponible")

            avisar(
                cita = cita,
                titulo = "Necesitas reprogramar tu cita",
                mensaje = "El profesional de tu cita de ${cita.servicio} del ${Fechas.legible(cita.fecha, cita.hora)} " +
                    "ya no está disponible. Entra a la app para elegir otro horario."
            )
        }

        Bitacora.info("Profesional $profesional desactivado: ${afectadas.size} citas afectadas")
        return afectadas.size
    }

    // ---------- auxiliares ----------

    private fun citaDelUsuario(
        usuario: UsuarioDto,
        citaId: Int
    ): CitaDto {
        val cita = CitaRepositorio.buscar(citaId)
            ?: throw ErrorNegocio(HttpStatusCode.NotFound, "La cita no existe.")

        // COP-RC-04: cada usuario solo ve y cambia sus propias citas
        if (cita.paciente != usuario.username) {
            throw ErrorNegocio(HttpStatusCode.Forbidden, "Esta cita no te pertenece.")
        }

        return cita
    }

    private fun validarPlazo(
        cita: CitaDto,
        accion: String
    ) {
        if (cita.estado !in EstadosCita.ACTIVOS) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Esta cita ya no se puede $accion porque está ${cita.estado.lowercase()}.")
        }

        if (!cita.puedeModificar) {
            throw ErrorNegocio(
                HttpStatusCode.Conflict,
                "Solo puedes $accion con al menos ${Configuracion.plazoHorasCambio} horas de anticipación. " +
                    "Para cambios de último momento llama al Centro: ${Configuracion.telefonoCentro}."
            )
        }
    }

    private fun validarHorarioLibre(
        profesional: String,
        fecha: String,
        hora: String,
        excluirCitaId: Int?
    ) {
        val dia = Fechas.fecha(fecha)
        Fechas.hora(hora)

        if (dia.isBefore(Fechas.hoy())) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "No se pueden agendar citas en fechas pasadas.")
        }

        if (dia.isAfter(Fechas.hoy().plusDays(DIAS_MAXIMOS))) {
            throw ErrorNegocio(HttpStatusCode.BadRequest, "Solo se puede agendar con un máximo de $DIAS_MAXIMOS días de anticipación.")
        }

        if (hora !in horasLibres(profesional, fecha, excluirCitaId)) {
            throw ErrorNegocio(HttpStatusCode.Conflict, "Ese horario ya no está disponible. Elige otro, por favor.")
        }
    }

    private fun avisar(
        cita: CitaDto,
        titulo: String,
        mensaje: String
    ) {
        // Las citas registradas por teléfono no tienen cuenta: no hay a quién avisar en la app
        if (cita.paciente.isBlank()) {
            return
        }

        NotificacionRepositorio.crear(cita.paciente, cita.id, titulo, mensaje)
    }

    private fun textoMotivo(motivo: String): String {
        return if (motivo.isBlank()) "" else " Motivo: ${motivo.trim()}."
    }
}
