package com.poligono.cop.data.remote

/**
 * "actual" para Android.
 * Se usa 127.0.0.1 junto con el comando "adb reverse tcp:8080 tcp:8080":
 * adb conecta el puerto 8080 DEL EMULADOR con el puerto 8080 de TU PC (donde corre el backend).
 * IMPORTANTE: el comando adb reverse se debe volver a correr cada vez que se prende el emulador.
 *
 * Alternativa sin adb reverse: "http://10.0.2.2:8080" (10.0.2.2 = tu PC vista desde el emulador).
 * Celular físico: "http://IP-DE-TU-PC:8080" (la ves con "ipconfig"), misma red Wi-Fi.
 */
actual fun urlServidor(): String = "http://127.0.0.1:8080"
