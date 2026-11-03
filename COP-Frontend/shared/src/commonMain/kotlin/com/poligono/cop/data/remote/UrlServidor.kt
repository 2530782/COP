package com.poligono.cop.data.remote

/**
 * "expect" = PROMESA: en commonMain decimos que existe esta función,
 * pero cada plataforma da su propia respuesta con "actual":
 *  - androidMain/.../UrlServidor.android.kt -> "http://10.0.2.2:8080"
 *  - iosMain/.../UrlServidor.ios.kt         -> "http://localhost:8080"
 *
 * ¿Por qué distinto? El emulador de Android es como otra computadora:
 * para él, "localhost" es el propio emulador. 10.0.2.2 es la dirección especial
 * con la que el emulador llega a TU PC (donde corre el backend Ktor en el puerto 8080).
 */
expect fun urlServidor(): String
