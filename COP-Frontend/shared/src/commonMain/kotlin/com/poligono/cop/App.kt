package com.poligono.cop

import androidx.compose.runtime.Composable
import com.poligono.cop.composeApp.navegacion.AppNavegacion
import com.poligono.cop.composeApp.tema.CopTheme
import com.poligono.cop.domain.biometria.AccesoBiometrico
import com.poligono.cop.domain.biometria.SinBiometria

/**
 * Raíz de la interfaz. La llaman:
 *  - Android: MainActivity (androidApp)
 *  - iOS:     MainViewController (iosMain)
 */
@Composable
fun App(acceso: AccesoBiometrico = SinBiometria) {
    CopTheme {
        AppNavegacion(acceso = acceso)
    }
}
