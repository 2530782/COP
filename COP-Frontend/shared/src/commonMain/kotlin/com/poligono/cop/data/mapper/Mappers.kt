package com.poligono.cop.data.mapper

import com.poligono.cop.data.dto.CitaDto
import com.poligono.cop.data.dto.ProfesionalDto
import com.poligono.cop.data.dto.ServicioDto
import com.poligono.cop.data.dto.UsuarioDto
import com.poligono.cop.domain.model.Cita
import com.poligono.cop.domain.model.EstadoCita
import com.poligono.cop.domain.model.Profesional
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
    servicioId = servicioId
)

fun ServicioDto.aDominio(): Servicio = Servicio(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    duracionMinutos = duracionMinutos,
    activo = activo
)

fun ProfesionalDto.aDominio(): Profesional = Profesional(
    username = username,
    nombre = nombre,
    servicioId = servicioId
)

fun CitaDto.aDominio(): Cita = Cita(
    id = id,
    servicio = servicio,
    profesional = profesionalNombre,
    paciente = pacienteNombre,
    fecha = fecha,
    hora = hora,
    estado = EstadoCita.desdeTexto(estado)
)
