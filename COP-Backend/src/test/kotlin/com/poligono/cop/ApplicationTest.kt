package com.poligono.cop

import com.poligono.cop.models.UsuarioRegistro
import com.poligono.cop.services.AuthService
import com.poligono.cop.services.ResultadoLogin
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Pruebas del backend. Se corren con:  ./gradlew test
 * testApplication levanta el servidor "de mentiritas" (sin abrir el puerto 8080).
 */
class ApplicationTest {

    @Test
    fun elServidorResponde() = testApplication {
        application { module() }
        val respuesta = client.get("/")
        assertEquals(HttpStatusCode.OK, respuesta.status)
    }

    @Test
    fun loginCorrectoRegresaElRol() = testApplication {
        application { module() }
        val respuesta = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"personal","password":"personal123"}""")
        }
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertTrue(respuesta.bodyAsText().contains("\"rol\": \"PERSONAL\""))
    }

    @Test
    fun loginIncorrectoRegresa401() = testApplication {
        application { module() }
        val respuesta = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"ana","password":"equivocada"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
    }

    @Test
    fun citasSeFiltranPorPaciente() = testApplication {
        application { module() }
        val texto = client.get("/citas?paciente=luis").bodyAsText()
        assertTrue(texto.contains("Luis Torres"))
        assertTrue(!texto.contains("Ana López"))
    }

    @Test
    fun tresIntentosFallidosBloqueanLaCuenta() {
        var ahora = 0L
        val auth = AuthService(
            usuarios = listOf(UsuarioRegistro("ana", "ana123", "Ana", "USUARIO")),
            reloj = { ahora }
        )
        assertIs<ResultadoLogin.Incorrecto>(auth.iniciarSesion("ana", "x"))
        assertIs<ResultadoLogin.Incorrecto>(auth.iniciarSesion("ana", "x"))
        assertIs<ResultadoLogin.Bloqueado>(auth.iniciarSesion("ana", "x"))
        // Aunque ahora ponga bien la contraseña, sigue bloqueada
        assertIs<ResultadoLogin.Bloqueado>(auth.iniciarSesion("ana", "ana123"))
        // Pasa un minuto y ya puede entrar
        ahora += AuthService.BLOQUEO_MS
        assertIs<ResultadoLogin.Exito>(auth.iniciarSesion("ana", "ana123"))
    }

    @Test
    fun cuentaDesactivadaNoEntra() {
        val auth = AuthService(listOf(UsuarioRegistro("pedro", "pedro123", "Pedro", "USUARIO", activo = false)))
        assertIs<ResultadoLogin.Desactivado>(auth.iniciarSesion("pedro", "pedro123"))
    }
}
