package com.poligono.cop.data.remote

/**
 * Dirección del backend para Android.
 *
 * SERVIDOR REMOTO -> USAR_SERVIDOR_REMOTO = true
 *   La app habla con el backend desplegado por SSH en el servidor Linux
 *   (ver COP-Backend/DESPLIEGUE.md). Pon en URL_SERVIDOR_REMOTO la IP pública
 *   del servidor con el puerto 8080, por ejemplo "http://20.120.33.45:8080".
 *   No hace falta adb reverse ni tener el backend en la PC; funciona en celular
 *   físico con datos móviles. Se permite HTTP porque el AndroidManifest tiene
 *   android:usesCleartextTraffic="true".
 *   Si se usa Render en su lugar: "https://cop-backend.onrender.com".
 *
 * SERVIDOR LOCAL (tu PC) -> USAR_SERVIDOR_REMOTO = false
 *   Se usa 127.0.0.1 junto con el comando "adb reverse tcp:8080 tcp:8080":
 *   adb conecta el puerto 8080 DEL EMULADOR con el puerto 8080 de TU PC.
 *   El comando se debe volver a correr cada vez que se prende el emulador.
 *   Alternativa sin adb reverse: "http://10.0.2.2:8080" (tu PC vista desde el emulador).
 */
const val USAR_SERVIDOR_REMOTO = true

// CAMBIAR por la IP pública de tu servidor
const val URL_SERVIDOR_REMOTO = "http://IP_DEL_SERVIDOR:8080"

const val URL_LOCAL = "http://127.0.0.1:8080"

actual fun urlServidor(): String {
    return if (USAR_SERVIDOR_REMOTO) {
        URL_SERVIDOR_REMOTO
    } else {
        URL_LOCAL
    }
}
