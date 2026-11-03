package com.poligono.cop.seguridad

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.poligono.cop.Configuracion
import java.util.Date

/**
 * Tokens JWT. Después del login la app manda en cada petición:
 *   Authorization: Bearer <token>
 * El token lleva el username y el rol, firmados para que no se puedan alterar.
 */
object Tokens {

    const val EMISOR = "cop-poligono-guadalupe"
    const val AUDIENCIA = "cop-app"
    const val REALM = "COP"

    private val algoritmo = Algorithm.HMAC256(Configuracion.secretoJwt)

    val verificador: JWTVerifier = JWT
        .require(algoritmo)
        .withIssuer(EMISOR)
        .withAudience(AUDIENCIA)
        .build()

    fun generar(
        username: String,
        rol: String
    ): String {
        val expira = Date(System.currentTimeMillis() + Configuracion.HORAS_TOKEN * 60 * 60 * 1000)

        return JWT.create()
            .withIssuer(EMISOR)
            .withAudience(AUDIENCIA)
            .withClaim("username", username)
            .withClaim("rol", rol)
            .withExpiresAt(expira)
            .sign(algoritmo)
    }

    /** Devuelve el username si el token es válido, o null. */
    fun usernameDesdeToken(token: String): String? {
        return try {
            verificador.verify(token).getClaim("username").asString()
        } catch (e: Exception) {
            null
        }
    }
}
