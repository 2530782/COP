package com.poligono.cop.data.mapper

import com.poligono.cop.data.dto.CitaDto
import com.poligono.cop.data.dto.ConfiguracionDto
import com.poligono.cop.data.dto.FechaDisponibleDto
import com.poligono.cop.data.dto.HorarioDto
import com.poligono.cop.data.dto.NotificacionDto
import com.poligono.cop.data.dto.PoligonoDto
import com.poligono.cop.data.dto.ProfesionalDto
import com.poligono.cop.data.dto.ReporteDto
import com.poligono.cop.data.dto.ServicioDto
import com.poligono.cop.data.dto.UsuarioDto
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.Configuracion
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.FechaDisponible
import com.poligono.cop.domain.model.Horario
import com.poligono.cop.domain.model.Notificacion
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.model.Profesional
import com.poligono.cop.domain.model.Reporte
import com.poligono.cop.domain.model.Rol
import com.poligono.cop.domain.model.Servicio
import com.poligono.cop.domain.model.Usuario

/*
 * MAPPERS: convierten un DTO (como llega de la red) en un modelo de dominio
 * (como lo usa la app). Ejemplo: el rol llega como texto "PERSONAL" y aquí
 * se convierte al enum Rol.PERSONAL.
 *
 * Son "funciones de extensión": se escriben como fun UsuarioDto.aDominio()
 * y se usan como  usuarioDto.aDominio()
 */

fun UsuarioDto.aDominio(): Usuario = Usuario(
    username = username,
    nombre = nombre,
    rol = Rol.desdeTexto(rol),
    servicioId = servicioId,
    telefono = telefono,
    correo = correo
)

fun ServicioDto.aDominio(): Servicio = Servicio(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    duracionMinutos = duracionMinutos,
    activo = activo
)

fun Servicio.aDto(): ServicioDto = ServicioDto(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    duracionMinutos = duracionMinutos,
    activo = activo
)

fun ProfesionalDto.aDominio(): Profesional = Profesional(
    username = username,
    nombre = nombre,
    servicioId = servicioId,
    activo = activo
)

fun CitaDto.aDominio(): Cita = Cita(
    id = id,
    servicio = servicio,
    profesional = profesionalNombre,
    paciente = pacienteNombre,
    fecha = fecha,
    hora = hora,
    estado = EstadoCita.desdeTexto(estado),
    servicioId = servicioId,
    profesionalUsername = profesional,
    pacienteUsername = paciente,
    pacienteTelefono = pacienteTelefono,
    notas = notas,
    motivo = motivo,
    creadoPor = creadoPor,
    puedeModificar = puedeModificar
)

fun HorarioDto.aDominio(): Horario = Horario(id, profesional, diaSemana, horaInicio, horaFin)

fun FechaDisponibleDto.aDominio(): FechaDisponible = FechaDisponible(fecha, espaciosLibres)

fun NotificacionDto.aDominio(): Notificacion = Notificacion(id, titulo, mensaje, leida, creado)

fun ReporteDto.aDominio(): Reporte = Reporte(id, usuario, descripcion, dispositivo, creado)

fun PoligonoDto.aDominio(): Poligono = Poligono(
    id = id,
    nombre = nombre,
    direccion = direccion,
    latitud = latitud,
    longitud = longitud,
    horario = horario,
    telefono = telefono,
    servicios = servicios,
    esPrincipal = esPrincipal,
    ubicacionAproximada = ubicacionAproximada
)

fun ConfiguracionDto.aDominio(): Configuracion = Configuracion(
    plazoHorasCambio = plazoHorasCambio,
    horasRecordatorio = horasRecordatorio,
    minutosInactividad = minutosInactividad,
    telefonoCentro = telefonoCentro,
    whatsappCentro = whatsappCentro
)
