# COP-Backend (Ktor Server)

Servidor de la app **COP – Citas Oficiales Polígono**. Está hecho con **Ktor** (framework de JetBrains, ligero y basado en corrutinas) y lee las credenciales predefinidas y los datos desde **archivos JSON**. Por ahora es una arquitectura **local**: no hay base de datos.

---

## 1. Cómo correrlo

1. Abre la carpeta `COP-Backend` en **IntelliJ IDEA** o **Android Studio** (File → Open).
2. Espera a que Gradle termine de sincronizar (descarga Ktor la primera vez).
3. Abre `src/main/kotlin/com/poligono/cop/Application.kt` y presiona el ▶ verde junto a `fun main()`.
   - O desde la terminal: `./gradlew run` (Windows: `gradlew.bat run`)
4. En la consola debe aparecer: `Responding at http://0.0.0.0:8080`
5. Prueba en el navegador: <http://localhost:8080/> → `Servidor COP funcionando`

Para correr las pruebas: `./gradlew test`

---

## 2. Estructura de carpetas

```
COP-Backend/
├── settings.gradle.kts          ← nombre del proyecto y de dónde se descargan las librerías (mavenCentral)
├── build.gradle.kts             ← plugins y dependencias de Ktor
├── gradle/libs.versions.toml    ← catálogo de versiones (Kotlin 2.4.20, Ktor 3.3.1)
└── src/
    ├── main/
    │   ├── kotlin/com/poligono/cop/
    │   │   ├── Application.kt       ← main(): arranca el servidor en el puerto 8080
    │   │   ├── Serialization.kt     ← plugin del SERIALIZADOR (JSON ⇄ clases)
    │   │   ├── HTTP.kt              ← CORS, manejo de errores y bitácora
    │   │   ├── Routing.kt           ← las RUTAS: /login, /servicios, /profesionales, /citas
    │   │   ├── models/              ← clases @Serializable (forma del JSON)
    │   │   │   ├── Login.kt         ← LoginRequest, LoginResponse, RespuestaError
    │   │   │   ├── Usuario.kt       ← UsuarioRegistro (con contraseña), UsuarioDto (sin), ProfesionalDto
    │   │   │   ├── Servicio.kt
    │   │   │   └── Cita.kt          ← CitaRegistro (como está en el JSON), CitaDto (como se responde)
    │   │   ├── data/
    │   │   │   └── JsonDataSource.kt ← lee los .json de resources/data
    │   │   └── services/
    │   │       └── AuthService.kt   ← lógica del login + bloqueo tras 3 intentos
    │   └── resources/
    │       ├── data/
    │       │   ├── usuarios.json    ← CREDENCIALES PREDEFINIDAS y roles
    │       │   ├── servicios.json   ← Fisioterapia, Nutrición, Psicología…
    │       │   └── citas.json       ← citas de ejemplo
    │       └── logback.xml          ← formato de los mensajes en consola
    └── test/kotlin/com/poligono/cop/
        └── ApplicationTest.kt       ← pruebas del servidor
```

La división `Application.kt`, `Routing.kt`, `Serialization.kt` y `HTTP.kt` es la misma que genera el **wizard de Ktor** (start.ktor.io): cada plugin vive en su propio archivo como una función de extensión `Application.configureXxx()`.

---

## 3. Explicación archivo por archivo

### `build.gradle.kts`
- `kotlinJvm`: compila Kotlin para la JVM (el servidor corre en la PC, no en el celular).
- `kotlinSerialization`: hace que `@Serializable` funcione. **Sin este plugin el serializador no genera código y truena.**
- `application` + `mainClass = "com.poligono.cop.ApplicationKt"`: agrega la tarea `run`. Kotlin compila el archivo `Application.kt` como una clase llamada `ApplicationKt`, por eso termina en `Kt`.
- Dependencias:
  | Dependencia | Para qué sirve |
  |---|---|
  | `ktor-server-core` | El núcleo de Ktor |
  | `ktor-server-netty` | El **motor** (engine) que realmente escucha el puerto y procesa HTTP. Otros motores: CIO, Jetty |
  | `ktor-server-content-negotiation` | Decide el formato de la respuesta (JSON) |
  | `ktor-serialization-kotlinx-json` | **El serializador**: kotlinx.serialization para JSON |
  | `ktor-server-cors` | Permite llamadas desde otra página web |
  | `ktor-server-call-logging` | Imprime cada petición en consola (bitácora) |
  | `ktor-server-status-pages` | Responde errores en JSON en lugar de una página fea |
  | `logback-classic` | Quien imprime los logs |

### `Application.kt`
```kotlin
embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
```
- `embeddedServer`: crea el servidor desde código (en el wizard es la opción “Configuration in: Code”).
- `host = "0.0.0.0"`: escucha en **todas** las interfaces de red. Si pusieras `"127.0.0.1"`, el emulador de Android no podría entrar.
- `wait = true`: el programa se queda corriendo hasta que lo detengas.
- `Application.module()`: aquí se arma todo en orden: leer JSON → instalar plugins → registrar rutas.

### `Serialization.kt` (la conexión con el serializador)
```kotlin
install(ContentNegotiation) { json(Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }) }
```
- **`install(...)`** así se agrega cualquier plugin en Ktor.
- **ContentNegotiation** + **json()**: a partir de aquí
  - `call.receive<LoginRequest>()` convierte el **JSON que llega → objeto Kotlin** (deserializar).
  - `call.respond(objeto)` convierte el **objeto Kotlin → JSON** (serializar).
- `encodeDefaults = true`: manda todos los campos aunque tengan su valor por defecto. Es importante porque el frontend espera, por ejemplo, el campo `activo` en cada servicio.

### `HTTP.kt`
- **CORS**: permite que una app web en otro dominio llame al servidor (pensado para integrarse con la BTP en el futuro).
- **StatusPages**: si llega un JSON mal escrito responde `400` con `{"exito": false, "mensaje": "..."}`; si algo truena, responde `500` y lo registra.
- **CallLogging**: imprime en consola algo como `200 OK: POST - /login`. Cubre el requisito COP-RS-06 (bitácora).

### `Routing.kt` (las rutas)
| Método | Ruta | Responde |
|---|---|---|
| GET | `/` | Texto: “Servidor COP funcionando” |
| POST | `/login` | `LoginResponse` con el usuario y su **rol** |
| GET | `/servicios` | Lista de servicios |
| GET | `/profesionales` | Usuarios con rol PROFESIONAL (sin contraseña) |
| GET | `/citas` | Todas las citas. `?paciente=ana` solo las de Ana. `?profesional=fisio` solo las del fisioterapeuta |

En `/login` se usa `when` sobre el resultado de `AuthService` y para cada caso se responde un **código HTTP** distinto:
| Caso | Código | Ejemplo de mensaje |
|---|---|---|
| Correcto | **200** OK | “Bienvenido, Ana López” |
| Contraseña mal | **401** Unauthorized | “… Te quedan 2 intento(s).” |
| 3 fallos seguidos | **423** Locked | “Cuenta bloqueada… Intenta de nuevo en 60 segundos.” |
| Cuenta desactivada | **403** Forbidden | “Tu cuenta está desactivada…” |
| Campos vacíos | **400** Bad Request | “Usuario y contraseña son obligatorios” |

### `models/`
Son clases `@Serializable`. Su forma es **la forma del JSON**. Los nombres de los campos son **idénticos** a los DTOs del frontend (`COP-Frontend/shared/.../data/dto`). Si uno cambia un nombre y el otro no, la conexión se rompe.

Hay dos versiones del usuario a propósito:
- `UsuarioRegistro`: como está en `usuarios.json`, **con contraseña**.
- `UsuarioDto`: lo que se manda al celular, **sin contraseña** (`aPublico()` hace la conversión).

Con las citas pasa algo parecido: `CitaRegistro` guarda solo ids (`"FIS"`, `"ana"`) y `CitaDto` ya trae los nombres (`"Fisioterapia"`, `"Ana López"`).

### `data/JsonDataSource.kt`
```kotlin
val usuarios: List<UsuarioRegistro> = leer("data/usuarios.json")
```
- `javaClass.classLoader.getResource(ruta)` busca el archivo **dentro de `src/main/resources`**.
- `json.decodeFromString<T>(texto)` **deserializa** el texto a la lista.
- `inline fun <reified T>`: `reified` permite saber el tipo `T` mientras el programa corre, así una sola función sirve para usuarios, servicios y citas.

### `services/AuthService.kt`
- Busca un usuario con ese `username` y `password` en la lista leída del JSON.
- Lleva la cuenta de fallos por usuario en un `HashMap`. Al tercer fallo seguido bloquea **1 minuto** (requisito COP-RNN-02).
- `sealed interface ResultadoLogin`: los 4 resultados posibles (`Exito`, `Incorrecto`, `Bloqueado`, `Desactivado`). Como es `sealed`, el `when` de `Routing.kt` está obligado a manejar los 4.
- `@Synchronized`: si llegan dos peticiones al mismo tiempo, se atienden una por una para que el contador no se desordene.

---

## 4. Cuentas de prueba (`usuarios.json`)

| Usuario | Contraseña | Rol | Vista que abre |
|---|---|---|---|
| `admin` | `1234` | ADMINISTRADOR | Panel de administrador |
| `personal` | `personal123` | PERSONAL | Agenda completa del Centro |
| `fisio` / `nutri` / `psico` | `fisio123` / `nutri123` / `psico123` | PROFESIONAL | Su propia agenda |
| `ana` / `luis` / `sofia` | `ana123` / `luis123` / `sofia123` | USUARIO | Sus citas y los servicios |
| `pedro` | `pedro123` | USUARIO (desactivado) | No entra: muestra “cuenta desactivada” |

Para agregar un usuario solo editas `usuarios.json` y reinicias el servidor; no se toca código (COP-RE-01).

---

## 5. Probar con Postman o Thunder Client

```
POST http://localhost:8080/login
Content-Type: application/json

{"username": "ana", "password": "ana123"}
```
Respuesta:
```json
{
    "exito": true,
    "mensaje": "Bienvenido, Ana López",
    "usuario": {
        "username": "ana",
        "nombre": "Ana López",
        "rol": "USUARIO",
        "servicioId": null
    }
}
```

---

## 6. Preguntas que te pueden hacer (y la respuesta corta)

1. **¿Qué motor usa el servidor?** Netty, en el puerto 8080 (`Application.kt`).
2. **¿Dónde están las credenciales?** En `src/main/resources/data/usuarios.json`. Las lee `JsonDataSource.kt`.
3. **¿Cómo se conecta el serializador?** En `Serialization.kt`, con `install(ContentNegotiation) { json() }`. Las clases se marcan con `@Serializable` y el plugin `kotlinSerialization` de Gradle genera el código de conversión.
4. **¿Qué pasa si escribes mal la contraseña 3 veces?** `AuthService` bloquea la cuenta 1 minuto y la ruta responde 423.
5. **¿Por qué el frontend no recibe la contraseña?** Porque se responde `UsuarioDto` (sin `password`), no `UsuarioRegistro`.
6. **¿Por qué `host = "0.0.0.0"`?** Para aceptar conexiones de otros dispositivos (emulador o celular), no solo de la misma PC.
