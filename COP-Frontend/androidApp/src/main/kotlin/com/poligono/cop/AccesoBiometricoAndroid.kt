package com.poligono.cop

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.poligono.cop.domain.biometria.AccesoBiometrico

/** Implementación Android de AccesoBiometrico (lector de huella con BiometricPrompt). */
class AccesoBiometricoAndroid(private val activity: FragmentActivity) : AccesoBiometrico {

    private val permitido = BiometricManager.Authenticators.BIOMETRIC_STRONG
    private val prefs = activity.applicationContext
        .getSharedPreferences("cop_huella", Context.MODE_PRIVATE)

    override fun disponible() =
        BiometricManager.from(activity).canAuthenticate(permitido) ==
            BiometricManager.BIOMETRIC_SUCCESS

    override fun autenticar(onExito: () -> Unit, onError: (String) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onExito()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onError(errString.toString())
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Iniciar sesión")
            .setSubtitle("Coloca tu dedo en el sensor")
            .setAllowedAuthenticators(permitido)
            .setNegativeButtonText("Usar contraseña")
            .build()
        prompt.authenticate(info)
    }

    // OJO: SharedPreferences NO está cifrado (suficiente para la demo; ver nota sobre Keystore)
    override fun guardarCredenciales(username: String, password: String) {
        prefs.edit().putString("user", username).putString("pass", password).apply()
    }

    override fun leerCredenciales(): Pair<String, String>? {
        val u = prefs.getString("user", null) ?: return null
        val p = prefs.getString("pass", null) ?: return null
        return u to p
    }

    override fun hayCredenciales() = leerCredenciales() != null
    override fun borrarCredenciales() = prefs.edit().clear().apply()
}
