package com.poligono.cop.composeApp.sensores

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada

/**
 * SENSOR DE GEOLOCALIZACIÓN (GPS) de COP.
 *
 * Etapas:
 * 1. Habilitación: se pide el permiso de ubicación y se revisa que el GPS esté encendido.
 * 2. Calibración: se descartan lecturas con precisión peor a 100 m (domain/ubicacion/FiltroUbicacion).
 * 3. Obtención: el sistema entrega latitud, longitud y precisión cada 3 segundos o 2 metros.
 * 4. Procesamiento: se promedian las últimas lecturas y se calcula la distancia a cada
 *    polígono (domain/usecase/PoligonosCercanosUseCase).
 *
 * "expect" = cada plataforma da su versión:
 *  - Android: androidMain/.../composeApp/sensores/SensorUbicacion.android.kt (LocationManager + osmdroid)
 *  - iOS: pendiente (COP-RE-02)
 */

/** Estado del sensor que usa la pantalla. */
class EstadoGps(
    val permisoConcedido: Boolean,
    val gpsEncendido: Boolean,
    val ubicacion: Coordenada?,
    val solicitarPermiso: () -> Unit,
    val abrirAjustesUbicacion: () -> Unit
)

/** Empieza a leer el GPS mientras [activo] sea true y la pantalla esté visible. */
@Composable
expect fun rememberSensorUbicacion(activo: Boolean): EstadoGps

/**
 * Mapa de OpenStreetMap con los polígonos y la ubicación de la persona.
 * [enfoque] mueve el mapa a ese punto cada vez que cambia.
 */
@Composable
expect fun MapaPoligonos(
    poligonos: List<Poligono>,
    ubicacion: Coordenada?,
    seleccionadoId: Int?,
    enfoque: Coordenada?,
    onSeleccionar: (Poligono) -> Unit,
    modifier: Modifier = Modifier
)
