@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.poligono.cop.data.remote

/** Dirección de la página actual, por ejemplo "https://cop-backend.onrender.com" */
private fun jsOrigenPagina(): String =
    js("window.location.origin")

/*
 * En web, la página la entrega el MISMO servidor de Render que tiene el API.
 * Por eso el API está en la misma dirección que la página
 * (no hay que escribir la URL a mano ni configurar CORS).
 */
actual fun urlServidor(): String {
    return jsOrigenPagina()
}