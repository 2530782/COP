# COP – Citas Oficiales Polígono

App móvil para agendar citas de fisioterapia, nutrición y psicología en el **Centro de Desarrollo Comunitario Polígono Guadalupe** (Tulancingo, Hgo.). Atiende el **ODS 3: Salud y bienestar**.

El proyecto está dividido en **dos partes independientes**:

| Carpeta | Qué es | Tecnología | Se abre con |
|---|---|---|---|
| `COP-Frontend/` | La app (pantallas por rol, huella, sensor GPS) | Kotlin Multiplatform + Compose Multiplatform + **Ktor Client** | Android Studio |
| `COP-Backend/` | El servidor (citas, usuarios, avisos, polígonos) | **Ktor Server** (Netty) + kotlinx.serialization + base de datos (H2 / PostgreSQL) | IntelliJ IDEA o Android Studio |

El backend está alojado en **Render** (servidor remoto, `render.yaml` + `COP-Backend/Dockerfile`)
y el proyecto usa **SSH** para GitHub y para entrar al servidor de Render (ver `SSH.md`).

## Cómo se conectan

```
┌──────────────────────────────┐        HTTP + JSON         ┌──────────────────────────────┐
│ COP-Frontend (teléfono)      │  POST /login               │ COP-Backend (PC o Render)    │
│                              │ ─────────────────────────▶ │                              │
│ Ktor Client (OkHttp)         │  {"username","password"}   │ Ktor Server (Netty)          │
│ + kotlinx.serialization      │                            │ + kotlinx.serialization      │
│                              │ ◀───────────────────────── │                              │
│ data/dto/*.kt                │  {"exito","mensaje",       │ models/*.kt                  │
│ data/local (sesión, caché)   │   "usuario", "token"}      │ base de datos (H2/PostgreSQL)│
│                              │                            │ datos iniciales: resources/  │
│                              │  Authorization: Bearer ... │   data/*.json                │
└──────────────────────────────┘ ─────────────────────────▶ └──────────────────────────────┘
```

Las dos partes se conectan por **tres** cosas que deben coincidir:
1. **La versión de Ktor**: `ktor = "3.3.1"` en el `gradle/libs.versions.toml` de cada proyecto.
2. **La dirección**: `UrlServidor.android.kt` del frontend apunta al backend: la URL de Render (`USAR_SERVIDOR_REMOTO = true`) o la PC con `adb reverse` (`false`).
3. **La forma del JSON**: los DTOs del frontend (`data/dto`) tienen **exactamente** los mismos campos que los modelos del backend (`models`).

## Orden para correrlo

1. **Backend en Render**: abre en el navegador `https://cop-backend.onrender.com/` (o tu dirección de Render) para despertarlo; debe decir `Servidor COP funcionando`. Pasos para desplegarlo en `COP-Backend/DESPLIEGUE.md`.
   - *En la PC (sin Render)*: abre `COP-Backend` y corre `Application.kt` (o `./gradlew run`), y en la app pon `USAR_SERVIDOR_REMOTO = false` y corre `adb reverse tcp:8080 tcp:8080`.
2. **Frontend**: abre `COP-Frontend` en Android Studio y ejecuta `androidApp` en el emulador o en el celular.
3. Inicia sesión (o crea una cuenta nueva desde la app):

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `1234` | Administrador |
| `personal` | `personal123` | Personal encargado |
| `fisio` / `nutri` / `psico` | `fisio123` / `nutri123` / `psico123` | Profesional |
| `ana` / `luis` / `sofia` | `ana123` / `luis123` / `sofia123` | Usuario (paciente) |

Para entrar con **huella**: inicia sesión una vez con "Recordar para entrar con huella" marcado.

## Rúbrica del examen práctico

| Aspecto (ponderación) | Cómo lo cumple el proyecto |
|---|---|
| Estructura, separación Backend/Frontend y alojamiento en servidor remoto (30 %) | Dos proyectos independientes `COP-Backend/` y `COP-Frontend/`; el frontend por capas (`composeApp` → `ui` → `domain` → `data`), el backend por `rutas` → `services` → `data`. Backend en **Render** con Docker y PostgreSQL (`render.yaml`, `DESPLIEGUE.md`); se entra por **SSH** (`SSH.md`). |
| Requerimiento no funcional: tecnologías y sensores (10 %) | Kotlin Multiplatform, Compose Multiplatform, Ktor Client/Server, kotlinx.serialization, JWT, PostgreSQL, Docker, Render. Sensores: **huella** (`AccesoBiometricoAndroid.kt`, BiometricPrompt) y **GPS** (`SensorUbicacion.android.kt`, LocationManager + mapa OpenStreetMap). |
| Requerimiento funcional: el sensor funciona (20 %) | Login con huella (`LoginScreen.kt`) y pantalla *Polígonos cercanos (GPS)* con el Polígono Guadalupe, la ubicación de la persona, el más cercano, la distancia y *Cómo llegar*. |
| Control de versiones: repositorio y cambios por ramas (10 %) | Git con ramas `main`, `backend` y `frontend`, unidas con merge; se sube a GitHub por SSH (`SSH.md`, sección 2). |
| Pregunta individual sobre tres archivos (30 %) | Guía de estudio en PDF (archivos principales explicados). |

## Sensor GPS

Pantalla **Polígonos cercanos (GPS)** (desde el login o desde cualquier panel): mapa de OpenStreetMap con los polígonos de Tulancingo (Guadalupe, La Cañada, Jaltepec y Metilatla), el **Polígono Guadalupe resaltado** como lugar de las citas, la ubicación de la persona y cuál le queda más cerca, con distancia y botón "Cómo llegar". Detalle por etapas (habilitación, calibración, obtención, procesamiento) en `COP-Frontend/README.md`. **Verificar las coordenadas en sitio** (la de Metilatla es aproximada).

## Requisitos del documento que cubre esta versión

| Requisito | Dónde |
|---|---|
| COP-RN-01 Registro, COP-RC-05 aviso de privacidad | `RegistroScreen`, `POST /registro` |
| COP-RN-02 Sesión, recuperar contraseña; COP-RNN-02 bloqueo tras 3 intentos | `LoginScreen` (con huella), `RecuperarScreen`, `AuthService.kt` |
| COP-RN-03 / 04 Servicios, profesionales y horarios | `AgendarScreen`, `/servicios`, `/profesionales`, `/disponibilidad` |
| COP-RN-05 Solicitar cita en 5 pasos (COP-RC-06) | `AgendarScreen`, `POST /citas` |
| COP-RN-06 / 07 Detalle, historial y próximas | `PanelUsuarioScreen` |
| COP-RN-08 / 09 Cancelar y reprogramar; COP-RNN-04 plazo | `PanelUsuarioScreen`, `ReprogramarScreen`, `ReglasCitas.kt` |
| COP-RN-10 Recordatorio 24 h; COP-RNN-07 aviso dentro de la app | `Avisos.kt`, `RecordatorioWorker` |
| COP-RN-11 Avisos de confirmación, cambio y cancelación | `NotificacionesScreen`, `RevisarAvisosWorker` |
| COP-RN-12 / 13 / 16 / 18 Agenda del personal y estados | `PanelPersonalScreen`, `PanelProfesionalScreen`, `PUT /citas/{id}/estado` |
| COP-RN-14 / 17 Horarios, servicios y profesionales; COP-RNN-05 | `AdminScreens.kt`, `ReglasCitas.profesionalNoDisponible` |
| COP-RN-15 Cita presencial o telefónica | `AgendarScreen` (modo manual), `POST /citas/manual` |
| COP-RNN-01 / 03 Sin duplicados ni fechas pasadas | `ReglasCitas.kt` |
| COP-RNN-06 Consulta sin conexión | `AlmacenLocal.kt`, `PanelViewModel` |
| COP-RNN-08 Cierre por inactividad | `NavegacionViewModel` |
| COP-RNN-09 Validación de formularios | `Validar` (frontend), `Validaciones.kt` (backend) |
| COP-RI-02 / 03 Notificaciones y sincronización (COP-RC-02, 30 s) | `NavegacionViewModel`, `Workers.kt` |
| COP-RI-04 Agregar al calendario | `PanelUsuarioScreen` |
| COP-RI-05 / 06 Pantallas adaptables y por rol | `Pantalla.segunRol()` + 4 paneles |
| COP-RI-07 Exportar agenda PDF y Excel | `ExportarScreen`, `/exportar/agenda.pdf|csv` |
| COP-RC-03 / 04 Contraseñas cifradas y confidencialidad | `Contrasenas.kt`, token JWT (`Seguridad.kt`) |
| COP-RC-07 / 10 Accesibilidad y coherencia | `Tema.kt`, `Componentes.kt` |
| COP-RE-01 Agregar servicios sin cambiar código | `CatalogoScreen` |
| COP-RS-06 Bitácora, COP-RS-07 reporte de fallas | tabla `bitacora` + `CallLogging`, `AyudaScreen` |

## Pendiente o por decidir

- **Push con Firebase (FCM)**: por ahora los avisos son notificaciones locales (la app revisa cada 30 s abierta y cada 15 min cerrada).
- **Correo para recuperar contraseña**: mientras tanto `MODO_DEMO=true` muestra el código en la app.
- **iOS**: lo básico compila; huella, mapa, GPS, calendario y notificaciones quedan pendientes (COP-RE-02).
- Confirmar con el Centro el plazo de cancelación (24 h, COP-RP-05), los horarios reales y las coordenadas de los polígonos.
