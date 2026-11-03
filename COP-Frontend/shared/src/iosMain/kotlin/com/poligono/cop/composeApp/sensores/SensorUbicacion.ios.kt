package com.poligono.cop.composeApp.sensores

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poligono.cop.domain.model.Poligono
import com.poligono.cop.domain.ubicacion.Coordenada

// Pendiente en iOS (COP-RE-02): CLLocationManager y MapKit. Por ahora se muestra la lista.

@Composable
actual fun rememberSensorUbicacion(activo: Boolean): EstadoGps {
    return EstadoGps(
        permisoConcedido = false,
        gpsEncendido = false,
        ubicacion = null,
        solicitarPermiso = {},
        abrirAjustesUbicacion = {}
    )
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
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("El mapa estará disponible en la versión para iOS.")
    }
}
