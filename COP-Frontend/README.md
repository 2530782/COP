# COP-Frontend (Kotlin Multiplatform + Compose Multiplatform)

App móvil **COP – Citas Oficiales Polígono**. Toda la interfaz y la lógica están en el módulo **`shared`** y se comparten entre Android e iOS. Para comunicarse con el backend usa **Ktor Client**.

## 0. Qué cambió en esta versión (noviembre 2026)

La arquitectura es la misma (`composeApp` → `ui` → `domain` → `data`) y **la huella sigue igual** (`AccesoBiometricoAndroid.kt` + checkbox "Recordar para entrar con huella"). Se agregó:

| Función | Requisito | Dónde |
|---|---|---|
| Crear cuenta con aviso de privacidad | RN-01, RC-05 | `CuentaScreens.kt`, `RegistroViewModel`, `RegistrarUsuarioUseCase` |
| Recuperar contraseña con código | RN-02 | `RecuperarScreen`, `RecuperarViewModel` |
| Sesión guardada (no pide contraseña cada vez) y cierre por inactividad | RN-02, RNN-08 | `data/local/AlmacenLocal.kt`, `NavegacionViewModel` |
| Agendar en 5 pasos | RN-03, 04, 05, RC-06 | `AgendarScreen`, `AgendarViewModel`, `AgendarCitaUseCase` |
| Cancelar y reprogramar dentro del plazo | RN-08, 09, RNN-04 | `PanelUsuarioScreen`, `ReprogramarScreen` |
| Recordatorio 24 h antes y avisos del Centro | RN-10, 11, RNN-07 | `data/local/Avisos.kt`, `plataforma/Workers.kt`, `NotificacionesScreen` |
| Agregar cita al calendario | RI-04 | `PanelUsuarioScreen` |
| Consulta sin internet | RNN-06 | `AlmacenLocal`, `PanelViewModel` |
| Confirmar, rechazar, modificar, cancelar, atendida/inasistencia | RN-12, 16, 18 | `AccionesAgenda.kt`, `PanelPersonalScreen`, `PanelProfesionalScreen` |
| Agenda con filtros, cita presencial | RN-13, 15 | `PanelPersonalScreen`, `AgendarScreen` (modo manual) |
| Servicios, profesionales y horarios | RN-14, 17, RNN-05 | `AdminScreens.kt`, `AdminViewModels.kt` |
| Exportar agenda PDF / Excel, reportes de fallas | RI-07, RS-07 | `AdminScreens.kt`, `AyudaScreen` |
| **Sensor GPS**: polígonos de Tulancingo en el mapa | — | `PoligonosScreen`, `composeApp/sensores/`, `domain/ubicacion/` |

### Sensor GPS (geolocalizador)

| Etapa | Qué hace | Archivo |
|---|---|---|
| Habilitación | Permisos `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` y revisa que el GPS esté encendido | `AndroidManifest.xml`, `SensorUbicacion.android.kt` |
| Calibración | Descarta lecturas con precisión peor a 100 m | `domain/ubicacion/FiltroUbicacion.kt` |
| Obtención | `LocationManager` (GPS y red) cada 3 s o 2 m: latitud, longitud y precisión | `SensorUbicacion.android.kt` |
| Procesamiento | Promedia las 3 últimas lecturas y calcula la distancia (Haversine) a cada polígono | `FiltroUbicacion.kt`, `PoligonosCercanosUseCase.kt` |

El mapa es **OpenStreetMap (osmdroid)**: libre y sin clave. Muestra el **Polígono Guadalupe** en rojo, los demás polígonos en azul y a la persona como punto azul; los ordena del más cercano al más lejano y tiene **Cómo llegar** y **Llamar**. Los polígonos vienen de `GET /poligonos` (y hay una copia en `data/local/PoligonosRespaldo.kt` para cuando no hay internet). La ubicación nunca se manda al servidor.

Pruebas del GPS: `shared/src/commonTest/.../UbicacionTest.kt`.

**Servidor en internet:** cambia la URL en `UrlServidor.android.kt` por la de Render o Railway. El tiempo de espera de `ApiClient.kt` subió a 60 s porque Render gratis tarda en "despertar".

---

## 1. Cómo correrlo

> Primero enciende el **COP-Backend** (ver su README). Si el servidor está apagado, la app muestra “No se pudo conectar con el servidor COP”.

1. Abre la carpeta `COP-Frontend` en **Android Studio** (File → Open).
2. Espera a que Gradle sincronice.
3. Arriba, en la configuración de ejecución, elige **androidApp** y tu emulador (ej. Pixel 9).
4. **Con el emulador ya prendido**, en la pestaña *Terminal* de Android Studio corre (cada vez que prendas el emulador):
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
   ```
   Debe responder `8080`. Así el `127.0.0.1:8080` del emulador llega al backend de tu PC.
5. Presiona ▶ Run.
6. Entra con una cuenta de prueba, por ejemplo `ana` / `ana123`.

**Si usas un celular físico** en lugar del emulador: cambia la dirección en
`shared/src/androidMain/kotlin/com/poligono/cop/data/remote/UrlServidor.android.kt`
por la IP de tu PC (la ves con `ipconfig`), por ejemplo `http://192.168.1.70:8080`. El celular y la PC deben estar en la misma red Wi-Fi.

Pruebas: `./gradlew :shared:testAndroidHostTest`

---

## 2. Estructura de carpetas

```
COP-Frontend/
├── settings.gradle.kts            ← incluye los módulos :androidApp y :shared
├── build.gradle.kts               ← declara los plugins (apply false)
├── gradle/libs.versions.toml      ← versiones y librerías (aquí se agregó Ktor Client)
├── androidApp/                    ← SOLO el arranque en Android
│   └── src/main/
│       ├── AndroidManifest.xml    ← permiso INTERNET + usesCleartextTraffic (para http://)
│       └── kotlin/.../MainActivity.kt  ← setContent { App() }
├── iosApp/                        ← SOLO el arranque en iOS (Xcode)
└── shared/                        ← TODO el código compartido
    ├── build.gradle.kts           ← dependencias por plataforma (Ktor OkHttp / Darwin)
    └── src/
        ├── commonMain/kotlin/com/poligono/cop/   ← código para TODAS las plataformas
        │   ├── App.kt                     ← raíz de la UI: CopTheme { AppNavegacion() }
        │   ├── composeApp/                ← PANTALLAS (Compose)
        │   │   ├── tema/Tema.kt           ← colores de la app
        │   │   ├── componentes/           ← piezas reutilizables (tarjetas, encabezado…)
        │   │   │   ├── Componentes.kt
        │   │   │   └── PanelBase.kt       ← esqueleto común de las 4 vistas de rol
        │   │   ├── navegacion/
        │   │   │   ├── Pantalla.kt        ← las rutas + ENRUTAMIENTO POR ROL
        │   │   │   └── AppNavegacion.kt   ← dibuja la pantalla actual
        │   │   └── pantallas/
        │   │       ├── LoginScreen.kt
        │   │       ├── PanelAdministradorScreen.kt
        │   │       ├── PanelPersonalScreen.kt
        │   │       ├── PanelProfesionalScreen.kt
        │   │       └── PanelUsuarioScreen.kt
        │   ├── ui/                        ← VIEWMODELS
        │   │   ├── LoginViewModel.kt
        │   │   ├── PanelViewModel.kt
        │   │   └── NavegacionViewModel.kt
        │   ├── domain/                    ← LÓGICA DE NEGOCIO PURA (sin red, sin pantallas)
        │   │   ├── model/                 ← Usuario, Rol, Servicio, Profesional, Cita
        │   │   ├── repository/CopRepository.kt   ← interfaz (contrato)
        │   │   └── usecase/               ← IniciarSesion, ObtenerCitas, ObtenerServicios…
        │   ├── data/                      ← DATOS (red con Ktor Client)
        │   │   ├── dto/                   ← clases @Serializable (forma del JSON)
        │   │   ├── remote/
        │   │   │   ├── ApiClient.kt       ← configuración del HttpClient de Ktor
        │   │   │   ├── CopApi.kt          ← una función por ruta del backend
        │   │   │   └── UrlServidor.kt     ← expect fun urlServidor()
        │   │   ├── mapper/Mappers.kt      ← DTO → modelo de dominio
        │   │   └── repository/CopRepositoryImpl.kt  ← implementación del contrato
        │   └── di/Dependencias.kt         ← arma todas las piezas una vez
        ├── androidMain/   ← UrlServidor.android.kt (actual) → http://10.0.2.2:8080
        ├── iosMain/       ← UrlServidor.ios.kt (actual) + MainViewController.kt
        └── commonTest/    ← RolTest.kt
```

Las carpetas `composeApp`, `data`, `domain` y `ui` dentro de `commonMain` siguen el mapa de estructura recomendado en el material del examen.

---

## 3. El viaje de un login, paso a paso

Ejemplo: Ana escribe `ana` / `ana123` y presiona **Entrar**.

```
LoginScreen ──entrar()──▶ LoginViewModel ──▶ IniciarSesionUseCase ──▶ CopRepositoryImpl ──▶ CopApi
   (composeApp)              (ui)                (domain)                  (data)            (data/remote)
                                                                                               │
                                                                     POST http://10.0.2.2:8080/login
                                                                     {"username":"ana","password":"ana123"}
                                                                                               ▼
                                                                                        COP-Backend (Ktor Server)
                                                                                               │
        PanelUsuarioScreen ◀── AppNavegacion ◀── NavegacionViewModel ◀── usuario con rol USUARIO ◀┘
```

1. **`LoginScreen`**: el botón llama a `viewModel.entrar()`.
2. **`LoginViewModel.entrar()`**: pone `cargando = true` (el botón muestra un circulito) y lanza una corrutina con `viewModelScope.launch`.
3. **`IniciarSesionUseCase`**: revisa que no haya campos vacíos (COP-RNN-09). Si falta algo, regresa el error **sin ir al servidor**.
4. **`CopRepositoryImpl.iniciarSesion()`**: llama a la API y revisa el código HTTP. Con 200 regresa el usuario; con 401, 423 o 403 regresa el mensaje del servidor; si no hay conexión, regresa “No se pudo conectar…”.
5. **`CopApi.login()`**: hace el `POST` con Ktor Client. `setBody(LoginRequest(...))` y el **serializador** lo convierte a JSON.
6. El backend responde JSON y `respuesta.body<LoginResponse>()` lo convierte de regreso a objeto Kotlin.
7. **`Mappers.kt`**: `UsuarioDto.aDominio()` convierte el texto `"USUARIO"` en `Rol.USUARIO`.
8. El ViewModel guarda `usuarioAutenticado` en su estado; `LoginScreen` lo detecta con `LaunchedEffect` y llama a `onLoginExitoso(usuario)`.
9. **`NavegacionViewModel.iniciarSesion(usuario)`** llama a `Pantalla.segunRol(usuario)`, que es **donde se enruta por rol**:
   ```kotlin
   when (usuario.rol) {
       Rol.ADMINISTRADOR -> PanelAdministrador(usuario)
       Rol.PERSONAL      -> PanelPersonal(usuario)
       Rol.PROFESIONAL   -> PanelProfesional(usuario)
       Rol.USUARIO       -> PanelUsuario(usuario)
   }
   ```
10. **`AppNavegacion`** ve que cambió `pantallaActual`, Compose redibuja (*recomposición*) y aparece **`PanelUsuarioScreen`**.

---

## 4. Conceptos clave explicados

### Las 4 capas (de afuera hacia adentro)
| Capa | Carpeta | ¿Qué hace? | ¿Sabe de Ktor? | ¿Sabe de Compose? |
|---|---|---|---|---|
| Pantallas | `composeApp` | Dibuja y recibe toques | No | Sí |
| ViewModels | `ui` | Guarda el estado y reacciona a eventos | No | No* |
| Dominio | `domain` | Reglas del negocio (validar, qué citas ve cada rol) | No | No |
| Datos | `data` | Habla con el backend | **Sí** | No |

\* `NavegacionViewModel` usa `mutableStateOf` de Compose solo para guardar la pantalla actual.

La ventaja: si mañana cambian Ktor por otra librería, solo se toca `data`. Las pantallas y el dominio quedan igual.

### `expect` / `actual` (código por plataforma)
En `commonMain` se declara la promesa:
```kotlin
expect fun urlServidor(): String
```
Cada plataforma la cumple a su manera:
- Android → `"http://10.0.2.2:8080"`: `10.0.2.2` es la forma en que el **emulador** llega a tu PC. Para el emulador, `localhost` sería el propio emulador.
- iOS → `"http://localhost:8080"`: el simulador de iOS sí comparte el `localhost` de la Mac.

### Ktor Client y el serializador (`ApiClient.kt`)
```kotlin
HttpClient {
    expectSuccess = false                    // 401/423 no lanzan excepción; nosotros revisamos el status
    install(ContentNegotiation) { json(...) } // SERIALIZADOR: JSON ⇄ clases @Serializable
    install(HttpTimeout) { ... }             // no esperar para siempre
}
```
- No se elige el motor aquí. Ktor lo toma de las dependencias de cada plataforma: **OkHttp** en Android (`androidMain`) y **Darwin** en iOS (`iosMain`). Ver `shared/build.gradle.kts`.
- `ignoreUnknownKeys = true`: si el backend agrega un campo nuevo, la app no truena.

### DTO vs. modelo de dominio
- **DTO** (`data/dto`): copia exacta del JSON. Por ejemplo, `rol: String`.
- **Modelo** (`domain/model`): lo que usa la app. Por ejemplo, `rol: Rol` (un enum).
- **Mapper**: convierte uno en otro. Así, si el JSON cambia, solo se ajusta el DTO y su mapper.

### ViewModel + StateFlow
```kotlin
private val _estado = MutableStateFlow(LoginUiState())   // solo el ViewModel lo modifica
val estado: StateFlow<LoginUiState> = _estado.asStateFlow() // la pantalla solo lo lee
```
En la pantalla, `val estado by viewModel.estado.collectAsState()` hace que cada cambio redibuje la UI automáticamente. El ViewModel **sobrevive al girar el teléfono**, por eso no se pierde lo escrito ni la sesión.

### `Result<T>`
Los repositorios regresan `Result.success(dato)` o `Result.failure(error)`. El ViewModel usa `.onSuccess { }` y `.onFailure { }`, sin `try/catch` regados por todos lados.

### `sealed`
`Pantalla` y `Rol` son listas **cerradas**. Si mañana agregas un rol nuevo (ej. `SUPERVISOR`), el compilador te marca error en cada `when` donde falte manejarlo, así no se te olvida ninguna vista.

---

## 5. Qué ve cada rol

| Rol | Pantalla | Contenido |
|---|---|---|
| ADMINISTRADOR | `PanelAdministradorScreen` | Resumen, agenda del Centro, servicios y profesionales, horarios, cita presencial, exportar, reportes de fallas, mapa |
| PERSONAL | `PanelPersonalScreen` | Solicitudes por confirmar (confirmar/rechazar), agenda con filtros (modificar, cancelar, atendida), cita presencial, horarios, exportar, mapa |
| PROFESIONAL | `PanelProfesionalScreen` | **Solo sus** citas, con el paciente; marca atendida o no asistió |
| USUARIO | `PanelUsuarioScreen` | Agendar, próximas citas (cancelar, reprogramar, calendario), historial, avisos, mapa de polígonos, ayuda |

Quién ve qué citas se decide en `domain/usecase/ObtenerCitasUseCase.kt` (confidencialidad, COP-RC-04).

---

## 6. Preguntas que te pueden hacer (y la respuesta corta)

1. **¿Dónde se configura Ktor Client?** En `data/remote/ApiClient.kt`. Las dependencias están en `shared/build.gradle.kts` y `gradle/libs.versions.toml`.
2. **¿Dónde se conecta el serializador en el frontend?** En `ApiClient.kt`, con `install(ContentNegotiation) { json() }`. Además, el plugin `kotlinSerialization` en `shared/build.gradle.kts` y las clases `@Serializable` en `data/dto`.
3. **¿Dónde se enruta por rol?** En `composeApp/navegacion/Pantalla.kt`, en la función `segunRol()`.
4. **¿Qué hay en `androidMain` y qué hay en `commonMain`?** `commonMain` tiene todo lo compartido. `androidMain` solo tiene lo exclusivo de Android: la URL, el motor OkHttp, el sensor GPS y el mapa (`composeApp/sensores`), y almacenamiento, notificaciones y recordatorios (`plataforma/`).
8. **¿Cómo funciona el sensor GPS?** Ver la tabla de la sección 0: habilitación (permiso), calibración (descarta lecturas imprecisas), obtención (`LocationManager`) y procesamiento (promedio y distancia a cada polígono).
9. **¿Dónde se guarda la sesión?** En `data/local/AlmacenLocal.kt` (SharedPreferences en Android). La huella guarda sus credenciales aparte en `AccesoBiometricoAndroid`.
5. **¿Por qué `10.0.2.2`?** Es la IP con la que el emulador de Android ve el `localhost` de la PC.
6. **¿Para qué sirve el ViewModel?** Guarda el estado de la pantalla, sobrevive a la rotación y hace las llamadas en `viewModelScope`.
7. **¿Qué es un DTO?** Una clase que representa exactamente el JSON que viaja por la red.
