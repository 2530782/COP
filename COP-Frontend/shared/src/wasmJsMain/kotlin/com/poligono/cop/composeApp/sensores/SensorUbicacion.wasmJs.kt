@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.poligono.cop.composeApp.sensores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada
import com.poligono.cop.plataforma.abrirEnlace
import kotlinx.coroutines.delay

/*
 * Implementación WEB del sensor de ubicación.
 * Usa la Geolocation API del navegador (navigator.geolocation).
 * La última lectura se guarda en window.copGps y Kotlin la revisa cada 3 segundos.
 */

// =====================================================================
// Funciones de JavaScript
// =====================================================================

private fun jsGpsDisponible(): Boolean =
    js("'geolocation' in navigator")

private fun jsIniciarGps(): Unit =
    js(
        """{
            window.copGps = window.copGps || { tiene: false, denegado: false, lat: 0, lng: 0 };
            if (!('geolocation' in navigator) || window.copGpsId != null) {
                return;
            }
            window.copGpsId = navigator.geolocation.watchPosition(
                function (pos) {
                    window.copGps.tiene = true;
                    window.copGps.denegado = false;
                    window.copGps.lat = pos.coords.latitude;
                    window.copGps.lng = pos.coords.longitude;
                },
                function (error) {
                    if (error.code === 1) {
                        window.copGps.denegado = true;
                    }
                },
                { enableHighAccuracy: true, maximumAge: 3000 }
            );
        }"""
    )

private fun jsDetenerGps(): Unit =
    js(
        """{
            if (window.copGpsId != null) {
                navigator.geolocation.clearWatch(window.copGpsId);
                window.copGpsId = null;
            }
        }"""
    )

private fun jsGpsTieneLectura(): Boolean =
    js("!!(window.copGps && window.copGps.tiene)")

private fun jsGpsDenegado(): Boolean =
    js("!!(window.copGps && window.copGps.denegado)")

private fun jsGpsLatitud(): Double =
    js("window.copGps ? window.copGps.lat : 0")

private fun jsGpsLongitud(): Double =
    js("window.copGps ? window.copGps.lng : 0")

// =====================================================================
// Sensor de ubicación
// =====================================================================

@Composable
actual fun rememberSensorUbicacion(activo: Boolean): EstadoGps {
    var ubicacion by remember { mutableStateOf<Coordenada?>(null) }
    var denegado by remember { mutableStateOf(false) }

    // Inicia el GPS del navegador y lo detiene al salir de la pantalla
    DisposableEffect(activo) {
        if (activo) {
            jsIniciarGps()
        }

        onDispose {
            jsDetenerGps()
        }
    }

    // Revisa la última lectura cada 3 segundos
    LaunchedEffect(activo) {
        while (activo) {
            denegado = jsGpsDenegado()

            if (jsGpsTieneLectura()) {
                ubicacion = Coordenada(
                    latitud = jsGpsLatitud(),
                    longitud = jsGpsLongitud()
                )
            }

            delay(3000)
        }
    }

    return EstadoGps(
        permisoConcedido = !denegado,
        gpsEncendido = jsGpsDisponible(),
        ubicacion = ubicacion,
        solicitarPermiso = { jsIniciarGps() },
        abrirAjustesUbicacion = {
            // En web el permiso se cambia desde el candado de la barra de direcciones
        }
    )
}

// =====================================================================
// Mapa
// =====================================================================

/*
 * osmdroid solo existe en Android, así que en web no se dibuja el mapa dentro de la app.
 * Se muestra una tarjeta con un botón que abre OpenStreetMap en otra pestaña.
 */
@Composable
actual fun MapaPoligonos(
    poligonos: List<Poligono>,
    ubicacion: Coordenada?,
    seleccionadoId: Int?,
    enfoque: Coordenada?,
    onSeleccionar: (Poligono) -> Unit,
    modifier: Modifier
) {
    // Centro del mapa: el punto enfocado, si no la ubicación de la persona, si no Tulancingo
    val centro = enfoque ?: ubicacion
    val latitud = centro?.latitud ?: 20.0833
    val longitud = centro?.longitud ?: -98.3667

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mapa de polígonos",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${poligonos.size} polígonos disponibles. " +
                        "El mapa interactivo está en la app móvil.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    abrirEnlace(
                        "https://www.openstreetmap.org/?mlat=$latitud&mlon=$longitud#map=16/$latitud/$longitud"
                    )
                }
            ) {
                Text("Abrir en OpenStreetMap")
            }
        }
    }
}