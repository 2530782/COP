package com.poligono.cop.composeApp.sensores

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.poligono.cop.data.local.PoligonosRespaldo
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada
import com.poligono.cop.domain.ubicacion.FiltroUbicacion
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

// ---------- 1. Habilitación ----------

private fun tienePermiso(contexto: Context): Boolean {
    val fino = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
    val aproximado = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)

    return fino == PackageManager.PERMISSION_GRANTED || aproximado == PackageManager.PERMISSION_GRANTED
}

private fun ubicacionEncendida(manager: LocationManager): Boolean {
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun aCoordenada(location: Location): Coordenada {
    return Coordenada(
        latitud = location.latitude,
        longitud = location.longitude,
        precisionMetros = if (location.hasAccuracy()) location.accuracy else 50f,
        momento = location.time
    )
}

@SuppressLint("MissingPermission")
@Composable
actual fun rememberSensorUbicacion(activo: Boolean): EstadoGps {
    val contexto = LocalContext.current
    val manager = remember {
        contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    var permiso by remember { mutableStateOf(tienePermiso(contexto)) }
    var encendida by remember { mutableStateOf(ubicacionEncendida(manager)) }
    var ubicacion by remember { mutableStateOf<Coordenada?>(null) }
    val filtro = remember { FiltroUbicacion() }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        permiso = resultado.values.any { it }
    }

    // Revisa cada 2 segundos si la persona activó el permiso o el GPS desde Ajustes
    LaunchedEffect(Unit) {
        while (true) {
            permiso = tienePermiso(contexto)
            encendida = ubicacionEncendida(manager)
            delay(2_000)
        }
    }

    // ---------- 3. Obtención ----------
    DisposableEffect(activo, permiso) {
        var escucha: LocationListener? = null

        if (activo && permiso) {
            filtro.reiniciar()

            // Primero la última ubicación conocida, para no esperar con el mapa vacío
            val ultima = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (ultima != null) {
                ubicacion = filtro.procesar(aCoordenada(ultima)) ?: ubicacion
            }

            val nuevaEscucha = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // ---------- 2. Calibración y 4. Procesamiento ----------
                    val procesada = filtro.procesar(aCoordenada(location))

                    if (procesada != null) {
                        ubicacion = procesada
                    }
                }

                override fun onProviderEnabled(provider: String) {
                    encendida = true
                }

                override fun onProviderDisabled(provider: String) {
                    encendida = ubicacionEncendida(manager)
                }

                // Necesario en Android 9 o anterior
                @Deprecated("Requerido por versiones antiguas de Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }
            }

            // Cada 3 segundos o cada 2 metros de movimiento
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3_000L,
                    2f,
                    nuevaEscucha,
                    Looper.getMainLooper()
                )
            }

            // La red da una primera ubicación más rápido dentro de casa
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3_000L,
                    2f,
                    nuevaEscucha,
                    Looper.getMainLooper()
                )
            }

            escucha = nuevaEscucha
        }

        onDispose {
            // Se deja de usar el GPS al salir de la pantalla (ahorro de batería, COP-RC-09)
            escucha?.let { manager.removeUpdates(it) }
        }
    }

    return EstadoGps(
        permisoConcedido = permiso,
        gpsEncendido = encendida,
        ubicacion = ubicacion,
        solicitarPermiso = {
            lanzador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        abrirAjustesUbicacion = {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            contexto.startActivity(intent)
        }
    )
}

// ---------- Mapa (osmdroid / OpenStreetMap) ----------

private const val COLOR_PRINCIPAL = 0xFFC62828.toInt()
private const val COLOR_POLIGONO = 0xFF1B5E7A.toInt()
private const val COLOR_SELECCIONADO = 0xFFF9A825.toInt()
private const val COLOR_USUARIO = 0xFF1E88E5.toInt()

private fun circulo(
    contexto: Context,
    color: Int,
    tamanoDp: Int
): GradientDrawable {
    val densidad = contexto.resources.displayMetrics.density
    val tamano = (tamanoDp * densidad).toInt()
    val borde = (3 * densidad).toInt()

    val dibujo = GradientDrawable()
    dibujo.shape = GradientDrawable.OVAL
    dibujo.setColor(color)
    dibujo.setStroke(borde, Color.WHITE)
    dibujo.setSize(tamano, tamano)

    return dibujo
}

@Composable
actual fun MapaPoligonos(
    poligonos: List<Poligono>,
    ubicacion: Coordenada?,
    seleccionadoId: Int?,
    enfoque: Coordenada?,
    onSeleccionar: (Poligono) -> Unit,
    modifier: Modifier
) {
    val contexto = LocalContext.current
    val onSeleccionarActual by rememberUpdatedState(onSeleccionar)

    val mapa = remember {
        // osmdroid pide identificar la app ante el servidor de mapas de OpenStreetMap
        // y guarda los mosaicos del mapa en la carpeta privada de la app
        val configuracion = Configuration.getInstance()
        configuracion.load(contexto, contexto.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        configuracion.userAgentValue = contexto.packageName
        configuracion.osmdroidBasePath = File(contexto.filesDir, "osmdroid")
        configuracion.osmdroidTileCache = File(configuracion.osmdroidBasePath, "tiles")

        val vista = MapView(contexto)
        vista.setTileSource(TileSourceFactory.MAPNIK)
        vista.setMultiTouchControls(true)
        vista.controller.setZoom(13.5)
        vista.controller.setCenter(GeoPoint(PoligonosRespaldo.principal.latitud, PoligonosRespaldo.principal.longitud))
        vista
    }

    DisposableEffect(Unit) {
        mapa.onResume()

        onDispose {
            mapa.onPause()
            mapa.onDetach()
        }
    }

    // Se vuelven a dibujar los marcadores cuando cambian los datos
    LaunchedEffect(poligonos, ubicacion, seleccionadoId) {
        mapa.overlays.clear()

        for (poligono in poligonos) {
            val color = when {
                poligono.id == seleccionadoId -> COLOR_SELECCIONADO
                poligono.esPrincipal -> COLOR_PRINCIPAL
                else -> COLOR_POLIGONO
            }

            val marcador = Marker(mapa)
            marcador.position = GeoPoint(poligono.latitud, poligono.longitud)
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marcador.icon = circulo(contexto, color, if (poligono.esPrincipal) 30 else 24)
            marcador.title = poligono.nombre
            marcador.snippet = poligono.direccion
            marcador.setOnMarkerClickListener { m, _ ->
                m.showInfoWindow()
                onSeleccionarActual(poligono)
                true
            }

            mapa.overlays.add(marcador)
        }

        if (ubicacion != null) {
            val yo = Marker(mapa)
            yo.position = GeoPoint(ubicacion.latitud, ubicacion.longitud)
            yo.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            yo.icon = circulo(contexto, COLOR_USUARIO, 18)
            yo.title = "Tú estás aquí"
            yo.snippet = "Precisión aproximada: ${ubicacion.precisionMetros.toInt()} m"

            mapa.overlays.add(yo)
        }

        mapa.invalidate()
    }

    LaunchedEffect(enfoque) {
        if (enfoque != null) {
            mapa.controller.animateTo(GeoPoint(enfoque.latitud, enfoque.longitud), 15.5, 800L)
        }
    }

    AndroidView(
        factory = { mapa },
        modifier = modifier
    )
}
