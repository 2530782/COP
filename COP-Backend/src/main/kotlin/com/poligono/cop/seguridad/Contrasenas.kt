package com.poligono.cop.seguridad

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * COP-RC-03: las contraseñas se guardan cifradas con PBKDF2 (incluido en Java),
 * nunca en texto plano. Formato guardado: pbkdf2$iteraciones$sal$hash
 */
object Contrasenas {

    private const val ITERACIONES = 120_000
    private const val LONGITUD_BITS = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA256"

    private val aleatorio = SecureRandom()

    fun cifrar(contrasena: String): String {
        val sal = ByteArray(16)
        aleatorio.nextBytes(sal)

        val hash = derivar(contrasena, sal, ITERACIONES)
        val codificador = Base64.getEncoder()

        return "pbkdf2\$$ITERACIONES\$${codificador.encodeToString(sal)}\$${codificador.encodeToString(hash)}"
    }

    fun verificar(
        contrasena: String,
        guardado: String
    ): Boolean {
        val partes = guardado.split("$")

        if (partes.size != 4 || partes[0] != "pbkdf2") {
            return false
        }

        val iteraciones = partes[1].toIntOrNull() ?: return false
        val decodificador = Base64.getDecoder()
        val sal = decodificador.decode(partes[2])
        val esperado = decodificador.decode(partes[3])
        val calculado = derivar(contrasena, sal, iteraciones)

        return MessageDigest.isEqual(esperado, calculado)
    }

    /** Código numérico de 6 dígitos para recuperar la contraseña. */
    fun codigoRecuperacion(): String {
        val numero = aleatorio.nextInt(1_000_000)
        return numero.toString().padStart(6, '0')
    }

    private fun derivar(
        contrasena: String,
        sal: ByteArray,
        iteraciones: Int
    ): ByteArray {
        val especificacion = PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, LONGITUD_BITS)
        val fabrica = SecretKeyFactory.getInstance(ALGORITMO)
        return fabrica.generateSecret(especificacion).encoded
    }
}
