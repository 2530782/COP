package com.poligono.cop.data

import java.net.URI
import java.net.URLDecoder
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/**
 * Acceso a la base de datos con JDBC.
 *
 * - En tu computadora (sin variable DATABASE_URL) usa H2 en el archivo ./datos/cop.mv.db,
 *   en modo compatible con PostgreSQL, así que no hay que instalar nada.
 * - En Render (o Railway) se crea una base PostgreSQL y se copia su URL
 *   en la variable DATABASE_URL del servicio web.
 */
object BaseDatos {

    private var url = ""
    private var usuario = ""
    private var contrasena = ""

    /** [urlPruebas]: base en memoria para las pruebas (ApplicationTest). */
    fun iniciar(urlPruebas: String? = null) {
        val databaseUrl = System.getenv("DATABASE_URL")

        if (urlPruebas != null) {
            url = urlPruebas
            usuario = "sa"
            contrasena = ""
        } else if (databaseUrl.isNullOrBlank()) {
            url = "jdbc:h2:./datos/cop;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
            usuario = "sa"
            contrasena = ""
        } else if (databaseUrl.startsWith("jdbc:")) {
            url = databaseUrl
            usuario = System.getenv("DB_USUARIO") ?: ""
            contrasena = System.getenv("DB_CONTRASENA") ?: ""
        } else {
            // Formato de Render: postgresql://usuario:contrasena@host:puerto/base
            val uri = URI(databaseUrl)
            val credenciales = (uri.rawUserInfo ?: "").split(":", limit = 2)
            val puerto = if (uri.port == -1) 5432 else uri.port
            val ssl = if (uri.host.contains(".render.com")) "?sslmode=require" else ""

            url = "jdbc:postgresql://${uri.host}:$puerto${uri.path}$ssl"
            usuario = URLDecoder.decode(credenciales.getOrElse(0) { "" }, "UTF-8")
            contrasena = URLDecoder.decode(credenciales.getOrElse(1) { "" }, "UTF-8")
        }

        Esquema.crearTablas()
        Esquema.cargarDatosIniciales()
    }

    fun conexion(): Connection {
        return DriverManager.getConnection(url, usuario, contrasena)
    }

    /** Ejecuta un SELECT y convierte cada fila con [mapear]. */
    fun <T> consultar(
        sql: String,
        vararg parametros: Any?,
        mapear: (ResultSet) -> T
    ): List<T> {
        conexion().use { con ->
            con.prepareStatement(sql).use { ps ->
                asignar(ps, parametros)

                ps.executeQuery().use { rs ->
                    val lista = mutableListOf<T>()

                    while (rs.next()) {
                        lista.add(mapear(rs))
                    }

                    return lista
                }
            }
        }
    }

    /** Ejecuta un UPDATE, DELETE o CREATE y devuelve las filas afectadas. */
    fun ejecutar(
        sql: String,
        vararg parametros: Any?
    ): Int {
        conexion().use { con ->
            con.prepareStatement(sql).use { ps ->
                asignar(ps, parametros)
                return ps.executeUpdate()
            }
        }
    }

    /** Ejecuta un INSERT y devuelve el id generado (la columna id debe ser la primera). */
    fun insertar(
        sql: String,
        vararg parametros: Any?
    ): Int {
        conexion().use { con ->
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                asignar(ps, parametros)
                ps.executeUpdate()

                ps.generatedKeys.use { claves ->
                    return if (claves.next()) claves.getInt(1) else 0
                }
            }
        }
    }

    /** Devuelve el primer valor numérico de una consulta, por ejemplo un COUNT(*). */
    fun entero(
        sql: String,
        vararg parametros: Any?
    ): Int {
        val resultado = consultar(sql, *parametros) { rs ->
            rs.getInt(1)
        }

        return resultado.firstOrNull() ?: 0
    }

    private fun asignar(
        ps: PreparedStatement,
        parametros: Array<out Any?>
    ) {
        parametros.forEachIndexed { indice, valor ->
            val posicion = indice + 1

            when (valor) {
                null -> ps.setNull(posicion, Types.NULL)
                is Int -> ps.setInt(posicion, valor)
                is Long -> ps.setLong(posicion, valor)
                is Double -> ps.setDouble(posicion, valor)
                is Boolean -> ps.setBoolean(posicion, valor)
                else -> ps.setString(posicion, valor.toString())
            }
        }
    }
}

/** Lee un entero que puede ser NULL. */
fun ResultSet.enteroONulo(columna: String): Int? {
    val valor = getInt(columna)
    return if (wasNull()) null else valor
}
