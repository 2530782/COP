package com.poligono.cop

import java.time.ZoneId

/**
 * Valores que se pueden cambiar sin tocar el código, con variables de entorno
 * (en Render o Railway: sección Environment / Variables del servicio).
 */
object Configuracion {

    // Render y Railway asignan el puerto en la variable PORT; en la PC se usa 8080
    val puerto: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080

    // Clave para firmar los tokens. En el servidor se debe poner una propia y larga.
    val secretoJwt: String = System.getenv("JWT_SECRETO") ?: "cop-clave-solo-para-desarrollo-cambiar-en-el-servidor"

    // Duración del token de sesión
    const val HORAS_TOKEN: Long = 8

    // Zona horaria del Centro (Tulancingo)
    val zona: ZoneId = ZoneId.of("America/Mexico_City")

    // COP-RNN-04 / COP-RP-05: plazo mínimo para cancelar o reprogramar (confirmar con el Centro)
    val plazoHorasCambio: Long = System.getenv("PLAZO_HORAS_CAMBIO")?.toLongOrNull() ?: 24

    // COP-RN-10: anticipación del recordatorio
    const val HORAS_RECORDATORIO: Int = 24

    // COP-RNN-08: minutos sin usar la app antes de cerrar la sesión
    val minutosInactividad: Int = System.getenv("MINUTOS_INACTIVIDAD")?.toIntOrNull() ?: 10

    // COP-RNN-02: bloqueo tras tres intentos fallidos seguidos
    const val MAX_INTENTOS: Int = 3
    val segundosBloqueo: Long = System.getenv("SEGUNDOS_BLOQUEO")?.toLongOrNull() ?: 60

    // Sin servicio de correo todavía: en modo demo el código de recuperación se regresa en la respuesta
    val modoDemo: Boolean = (System.getenv("MODO_DEMO") ?: "true").lowercase() == "true"

    // Contacto que la app muestra en Ayuda (COP-RS-07)
    val telefonoCentro: String = System.getenv("TELEFONO_CENTRO") ?: "775 755 8450 ext. 1135"
    val whatsappCentro: String = System.getenv("WHATSAPP_CENTRO") ?: "7753599248"
}
