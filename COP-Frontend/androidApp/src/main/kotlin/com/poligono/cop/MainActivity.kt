package com.poligono.cop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.poligono.cop.plataforma.ContextoAndroid

/**
 * Punto de entrada en Android. Muestra App(), que vive en el módulo shared.
 * Extiende FragmentActivity (y no ComponentActivity) porque BiometricPrompt lo necesita.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Almacenamiento local, notificaciones, recordatorios y GPS necesitan el contexto de la app
        ContextoAndroid.iniciar(applicationContext)

        val acceso = AccesoBiometricoAndroid(this)

        setContent {
            App(acceso)
        }
    }
}
