package com.poligono.cop.data.remote

/**
 * Dirección del backend para Android.
 *
 * SERVIDOR REMOTO (Render) -> USAR_SERVIDOR_REMOTO = true
 *   La app habla con el backend alojado en Render. No hace falta adb reverse
 *   ni tener el backend corriendo en la PC; funciona en celular físico con datos.
 *   Pon en URL_RENDER la dirección que te da Render (ver COP-Backend/DESPLIEGUE.md).
 *   OJO: el plan gratis se duerme; la primera petición puede tardar ~1 min.
 *
 * SERVIDOR LOCAL (tu PC) -> USAR_SERVIDOR_REMOTO = false
 *   Se usa 127.0.0.1 junto con el comando "adb reverse tcp:8080 tcp:8080":
 *   adb conecta el puerto 8080 DEL EMULADOR con el puerto 8080 de TU PC.
 *   El comando se debe volver a correr cada vez que se prende el emulador.
 *   Alternativa sin adb reverse: "http://10.0.2.2:8080" (tu PC vista desde el emulador).
 */
const val USAR_SERVIDOR_REMOTO = true

const val URL_RENDER = "https://cop-backend.onrender.com"

const val URL_LOCAL = "http://127.0.0.1:8080"

actual fun urlServidor(): String {
    return if (USAR_SERVIDOR_REMOTO) {
        URL_RENDER
    } else {
        URL_LOCAL
    }
}
