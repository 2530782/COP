package com.poligono.cop.domain.biometria

/**
 * Contrato para entrar con huella. Vive en el código compartido (commonMain);
 * cada plataforma da su implementación (Android: BiometricPrompt).
 */
interface AccesoBiometrico {
    /** true si el equipo tiene sensor y al menos una huella registrada */
    fun disponible(): Boolean
    fun autenticar(onExito: () -> Unit, onError: (String) -> Unit)

    fun guardarCredenciales(username: String, password: String)
    fun leerCredenciales(): Pair<String, String>?
    fun hayCredenciales(): Boolean
    fun borrarCredenciales()
}

/** Para plataformas sin huella (por ahora iOS): la app funciona igual, sin el botón. */
object SinBiometria : AccesoBiometrico {
    override fun disponible() = false
    override fun autenticar(onExito: () -> Unit, onError: (String) -> Unit) =
        onError("Huella no disponible")
    override fun guardarCredenciales(username: String, password: String) {}
    override fun leerCredenciales(): Pair<String, String>? = null
    override fun hayCredenciales() = false
    override fun borrarCredenciales() {}
}
