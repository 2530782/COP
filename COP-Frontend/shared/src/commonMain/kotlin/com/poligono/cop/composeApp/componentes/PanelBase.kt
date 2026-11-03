package com.poligono.cop.composeApp.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poligono.cop.domain.model.Usuario
import com.poligono.cop.ui.PanelUiState

/**
 * "Esqueleto" que comparten las 4 vistas de rol:
 *   encabezado + (cargando | error | contenido en lista con scroll)
 *
 * El parámetro "contenido" es una función: cada pantalla de rol
 * manda SUS secciones y aquí se colocan dentro de la lista.
 */
@Composable
fun PanelBase(
    usuario: Usuario,
    estado: PanelUiState,
    onActualizar: () -> Unit,
    onCerrarSesion: () -> Unit,
    contenido: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = { EncabezadoPanel(usuario, onCerrarSesion) },
        containerColor = MaterialTheme.colorScheme.background
    ) { espacioInterno ->
        when {
            // 1) Cargando por primera vez
            estado.cargando -> Box(
                modifier = Modifier.fillMaxSize().padding(espacioInterno),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando información…")
                }
            }

            // 2) Hubo un error (ej. backend apagado)
            estado.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(espacioInterno).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MensajeError(texto = estado.error, onReintentar = onActualizar)
            }

            // 3) Todo bien: lista con scroll (LazyColumn solo dibuja lo visible)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(espacioInterno),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contenido()

                item {
                    TextButton(
                        onClick = onActualizar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar")
                    }
                }
            }
        }
    }
}
