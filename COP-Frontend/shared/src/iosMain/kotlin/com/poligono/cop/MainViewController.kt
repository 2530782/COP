package com.poligono.cop

import androidx.compose.ui.window.ComposeUIViewController

/** Punto de entrada para iOS: envuelve la UI de Compose en un UIViewController. */
fun MainViewController() = ComposeUIViewController { App() }
