package com.poligono.cop.rutas

import com.lowagie.text.Document
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.poligono.cop.consulta
import com.poligono.cop.data.CitaRepositorio
import com.poligono.cop.data.FiltroCitas
import com.poligono.cop.data.UsuarioRepositorio
import com.poligono.cop.models.CitaDto
import com.poligono.cop.models.Roles
import com.poligono.cop.requerirRol
import com.poligono.cop.seguridad.Tokens
import com.poligono.cop.services.ErrorNegocio
import com.poligono.cop.services.Fechas
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.ByteArrayOutputStream

/**
 * COP-RI-07: exportar la agenda por periodo y por servicio en PDF y en Excel (CSV).
 * Como la app abre estos enlaces en el navegador del teléfono, el token puede ir
 * en el parámetro ?token= además del encabezado Authorization.
 */
fun Route.rutasExportar() {

    get("/exportar/agenda.csv") {
        val citas = citasParaExportar(call)
        val texto = StringBuilder()

        // BOM para que Excel reconozca los acentos
        texto.append('﻿')
        texto.append("Fecha,Hora,Servicio,Profesional,Paciente,Teléfono,Estado,Notas\n")

        for (c in citas) {
            val columnas = listOf(
                c.fecha,
                c.hora,
                c.servicio,
                c.profesionalNombre,
                c.pacienteNombre,
                c.pacienteTelefono,
                c.estado,
                c.notas
            )

            texto.append(columnas.joinToString(",") { csv(it) })
            texto.append("\n")
        }

        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=agenda_cop.csv")
        call.respondBytes(
            texto.toString().toByteArray(Charsets.UTF_8),
            ContentType.parse("text/csv; charset=utf-8")
        )
    }

    get("/exportar/agenda.pdf") {
        val citas = citasParaExportar(call)
        val salida = ByteArrayOutputStream()
        val documento = Document(PageSize.LETTER.rotate())

        PdfWriter.getInstance(documento, salida)
        documento.open()

        val titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
        val negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)
        val normal = FontFactory.getFont(FontFactory.HELVETICA, 9f)

        documento.add(Paragraph("Agenda COP - Centro de Desarrollo Comunitario Polígono Guadalupe", titulo))
        documento.add(Paragraph(descripcionPeriodo(call), normal))
        documento.add(Paragraph(" ", normal))

        val encabezados = listOf("Fecha", "Hora", "Servicio", "Profesional", "Paciente", "Teléfono", "Estado")
        val tabla = PdfPTable(encabezados.size)
        tabla.widthPercentage = 100f

        for (encabezado in encabezados) {
            tabla.addCell(PdfPCell(Phrase(encabezado, negrita)))
        }

        for (c in citas) {
            tabla.addCell(PdfPCell(Phrase(c.fecha, normal)))
            tabla.addCell(PdfPCell(Phrase(c.hora, normal)))
            tabla.addCell(PdfPCell(Phrase(c.servicio, normal)))
            tabla.addCell(PdfPCell(Phrase(c.profesionalNombre, normal)))
            tabla.addCell(PdfPCell(Phrase(c.pacienteNombre, normal)))
            tabla.addCell(PdfPCell(Phrase(c.pacienteTelefono, normal)))
            tabla.addCell(PdfPCell(Phrase(c.estado, normal)))
        }

        documento.add(tabla)
        documento.add(Paragraph(" ", normal))
        documento.add(Paragraph("Total de citas: ${citas.size}", negrita))
        documento.close()

        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=agenda_cop.pdf")
        call.respondBytes(salida.toByteArray(), ContentType.Application.Pdf)
    }
}

private fun citasParaExportar(call: ApplicationCall): List<CitaDto> {
    val encabezado = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
    val token = call.consulta("token") ?: encabezado
        ?: throw ErrorNegocio(HttpStatusCode.Unauthorized, "Inicia sesión para exportar.")

    val username = Tokens.usernameDesdeToken(token)
        ?: throw ErrorNegocio(HttpStatusCode.Unauthorized, "Tu sesión expiró. Inicia sesión de nuevo.")

    val usuario = UsuarioRepositorio.buscar(username)?.aPublico()
        ?: throw ErrorNegocio(HttpStatusCode.Unauthorized, "Sesión no válida.")

    usuario.requerirRol(*Roles.PERSONAL_DEL_CENTRO)

    val desde = call.consulta("desde") ?: Fechas.hoy().toString()
    val hasta = call.consulta("hasta") ?: Fechas.hoy().plusDays(30).toString()

    return CitaRepositorio.listar(
        FiltroCitas(
            desde = desde,
            hasta = hasta,
            servicioId = call.consulta("servicioId"),
            profesional = call.consulta("profesional")
        )
    )
}

private fun descripcionPeriodo(call: ApplicationCall): String {
    val desde = call.consulta("desde") ?: Fechas.hoy().toString()
    val hasta = call.consulta("hasta") ?: Fechas.hoy().plusDays(30).toString()
    return "Periodo: $desde a $hasta"
}

private fun csv(valor: String): String {
    val limpio = valor.replace("\"", "\"\"")
    return "\"$limpio\""
}
