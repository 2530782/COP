package com.poligono.cop.domain.model

/**
 * Roles (perfiles de trabajo) de la app COP.
 * Basado en COP-RI-06: interfaz diferenciada para usuario, personal encargado y profesional.
 * Se agrega ADMINISTRADOR para la gestión general del Centro.
 *
 * El texto de cada rol (ej. "PERSONAL") es EXACTAMENTE el que viene en usuarios.json del backend.
 */
enum class Rol(val etiqueta: String) {
    ADMINISTRADOR("Administrador"),
    PERSONAL("Personal encargado"),
    PROFESIONAL("Profesional"),
    USUARIO("Usuario");

    companion object {
        /**
         * Convierte el texto que manda el servidor en un Rol.
         * Si llega algo desconocido se usa USUARIO, que es el rol con MENOS permisos
         * (así nunca se le da de más a alguien por error).
         */
        fun desdeTexto(texto: String?): Rol =
            entries.firstOrNull { it.name.equals(texto, ignoreCase = true) } ?: USUARIO
    }
}
