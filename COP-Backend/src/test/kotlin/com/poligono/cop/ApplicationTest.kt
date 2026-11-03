package com.poligono.cop

import com.poligono.cop.data.BaseDatos
import com.poligono.cop.services.AuthService
import com.poligono.cop.services.ResultadoLogin
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Pruebas del backend. Se corren con:  ./gradlew test
 * testApplication levanta el servidor "de mentiritas" (sin abrir el puerto 8080)
 * y cada prueba usa su propia base de datos EN MEMORIA, con los datos de resources/data.
 */
class ApplicationTest {

    private var contador = 0

    private fun baseNueva(): String {
        contador++
        return "jdbc:h2:mem:prueba${System.nanoTime()}$contador;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    }

    /** Inicia sesión y regresa el token. */
    private suspend fun token(cliente: HttpClient, usuario: String, password: String): String {
        val respuesta = cliente.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$usuario","password":"$password"}""")
        }
        val json = Json.parseToJsonElement(respuesta.bodyAsText()).jsonObject
        return json["token"]!!.jsonPrimitive.content
    }

    @Test
    fun elServidorResponde() = testApplication {
        val url = baseNueva()
        application { module(url) }
        val respuesta = client.get("/")
        assertEquals(HttpStatusCode.OK, respuesta.status)
    }

    @Test
    fun loginCorrectoRegresaElRolYToken() = testApplication {
        val url = baseNueva()
        application { module(url) }
        val respuesta = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"personal","password":"personal123"}""")
        }
        assertEquals(HttpStatusCode.OK, respuesta.status)
        val texto = respuesta.bodyAsText()
        assertTrue(texto.contains("\"rol\": \"PERSONAL\""))
        assertTrue(texto.contains("\"token\""))
    }

    @Test
    fun loginIncorrectoRegresa401() = testApplication {
        val url = baseNueva()
        application { module(url) }
        val respuesta = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"ana","password":"equivocada"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
    }

    @Test
    fun citasPidenSesion() = testApplication {
        val url = baseNueva()
        application { module(url) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/citas").status)
    }

    @Test
    fun cadaUsuarioSoloVeSusCitas() = testApplication {
        val url = baseNueva()
        application { module(url) }
        val tokenLuis = token(client, "luis", "luis123")

        // Aunque pida las de Ana, el servidor solo le regresa las suyas (COP-RC-04)
        val texto = client.get("/citas?paciente=ana") {
            header(HttpHeaders.Authorization, "Bearer $tokenLuis")
        }.bodyAsText()

        assertTrue(texto.contains("Luis Torres"))
        assertTrue(!texto.contains("Ana López"))
    }

    @Test
    fun agendarYNoDuplicar() = testApplication {
        val url = baseNueva()
        application { module(url) }
        val tokenAna = token(client, "ana", "ana123")
        val tokenSofia = token(client, "sofia", "sofia123")

        val fechas = Json.parseToJsonElement(
            client.get("/disponibilidad/fechas?profesional=fisio") {
                header(HttpHeaders.Authorization, "Bearer $tokenAna")
            }.bodyAsText()
        ).jsonArray
        val fecha = fechas.first().jsonObject["fecha"]!!.jsonPrimitive.content

        val horas = Json.parseToJsonElement(
            client.get("/disponibilidad?profesional=fisio&fecha=$fecha") {
                header(HttpHeaders.Authorization, "Bearer $tokenAna")
            }.bodyAsText()
        ).jsonArray
        val hora = horas.first().jsonPrimitive.content

        val cuerpo = """{"profesional":"fisio","fecha":"$fecha","hora":"$hora"}"""

        val primera = client.post("/citas") {
            header(HttpHeaders.Authorization, "Bearer $tokenAna")
            contentType(ContentType.Application.Json)
            setBody(cuerpo)
        }
        assertEquals(HttpStatusCode.Created, primera.status)

        // COP-RNN-01: nadie más puede tomar ese mismo horario
        val segunda = client.post("/citas") {
            header(HttpHeaders.Authorization, "Bearer $tokenSofia")
            contentType(ContentType.Application.Json)
            setBody(cuerpo)
        }
        assertEquals(HttpStatusCode.Conflict, segunda.status)
    }

    @Test
    fun tresIntentosFallidosBloqueanLaCuenta() {
        BaseDatos.iniciar(baseNueva())
        var ahora = System.currentTimeMillis()
        val auth = AuthService(reloj = { ahora })

        assertIs<ResultadoLogin.Incorrecto>(auth.iniciarSesion("ana", "x"))
        assertIs<ResultadoLogin.Incorrecto>(auth.iniciarSesion("ana", "x"))
        assertIs<ResultadoLogin.Bloqueado>(auth.iniciarSesion("ana", "x"))
        // Aunque ahora ponga bien la contraseña, sigue bloqueada
        assertIs<ResultadoLogin.Bloqueado>(auth.iniciarSesion("ana", "ana123"))
        // Pasa el tiempo de bloqueo y ya puede entrar
        ahora += Configuracion.segundosBloqueo * 1000
        assertIs<ResultadoLogin.Exito>(auth.iniciarSesion("ana", "ana123"))
    }

    @Test
    fun cuentaDesactivadaNoEntra() {
        BaseDatos.iniciar(baseNueva())
        val auth = AuthService()
        assertIs<ResultadoLogin.Desactivado>(auth.iniciarSesion("pedro", "pedro123"))
    }
}
