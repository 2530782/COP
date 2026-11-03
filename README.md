# COP – Citas Oficiales Polígono

App móvil para agendar citas de fisioterapia, nutrición y psicología en el **Centro de Desarrollo Comunitario Polígono Guadalupe** (Tulancingo, Hgo.). Atiende el **ODS 3: Salud y bienestar**.

El proyecto está dividido en **dos partes independientes**:

| Carpeta | Qué es | Tecnología | Se abre con |
|---|---|---|---|
| `COP-Frontend/` | La app (pantallas, navegación por rol) | Kotlin Multiplatform + Compose Multiplatform + **Ktor Client** | Android Studio |
| `COP-Backend/` | El servidor (login y datos desde JSON) | **Ktor Server** (Netty) + kotlinx.serialization | IntelliJ IDEA o Android Studio |

## Cómo se conectan

```
┌──────────────────────────────┐        HTTP + JSON         ┌──────────────────────────────┐
│ COP-Frontend (emulador)      │  POST /login               │ COP-Backend (tu PC :8080)    │
│                              │ ─────────────────────────▶ │                              │
│ Ktor Client (OkHttp)         │  {"username","password"}   │ Ktor Server (Netty)          │
│ + kotlinx.serialization      │                            │ + kotlinx.serialization      │
│                              │ ◀───────────────────────── │                              │
│ data/dto/*.kt                │  {"exito","mensaje",       │ models/*.kt                  │
│                              │   "usuario":{..."rol"}}    │ resources/data/usuarios.json │
└──────────────────────────────┘                            └──────────────────────────────┘
```

Las dos partes se conectan por **tres** cosas que deben coincidir:
1. **La versión de Ktor**: `ktor = "3.3.1"` en el `gradle/libs.versions.toml` de cada proyecto.
2. **La dirección**: el frontend llama a `http://10.0.2.2:8080` y el backend escucha en el puerto `8080`.
3. **La forma del JSON**: los DTOs del frontend (`data/dto`) tienen **exactamente** los mismos campos que los modelos del backend (`models`).

## Orden para correrlo

1. **Backend**: abre `COP-Backend` y corre `Application.kt` (o `./gradlew run`). Comprueba <http://localhost:8080/>.
2. **Frontend**: abre `COP-Frontend` en Android Studio y corre `androidApp` en el emulador.
3. Inicia sesión:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `1234` | Administrador |
| `personal` | `personal123` | Personal encargado |
| `fisio` | `fisio123` | Profesional (Fisioterapia) |
| `ana` | `ana123` | Usuario (paciente) |

Cada README explica su proyecto archivo por archivo e incluye preguntas de examen con su respuesta.

## Requisitos del documento que ya cubre esta versión

| Requisito | Dónde |
|---|---|
| COP-RN-02 Iniciar y cerrar sesión | `LoginScreen`, botón “Salir” de cada panel |
| COP-RN-03 / 04 Servicios y profesionales | `/servicios`, `/profesionales`, `PanelUsuarioScreen` |
| COP-RN-06 / 07 Detalle, historial y próximas citas | `TarjetaCita`, `PanelUsuarioScreen` |
| COP-RN-13 Agenda del personal | `PanelPersonalScreen` |
| COP-RNN-02 Bloqueo tras 3 intentos | `AuthService.kt` (backend) |
| COP-RNN-09 Validación de formularios | `IniciarSesionUseCase.kt` y `/login` |
| COP-RI-05 Distintos tamaños de pantalla | `LazyColumn` + `widthIn(max = 480.dp)` en el login |
| COP-RI-06 Interfaz diferenciada por rol | `Pantalla.segunRol()` + 4 pantallas |
| COP-RC-04 Confidencialidad | `ObtenerCitasUseCase` + filtros de `/citas` |
| COP-RS-06 Bitácora | `CallLogging` en `HTTP.kt` |

Agendar, cancelar y reprogramar citas (COP-RN-05, 08 y 09) quedan para la siguiente iteración.
