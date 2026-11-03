# Despliegue de COP-Backend en Railway (PaaS)

## 1. Servidor elegido: Railway

**Railway** es una Plataforma como Servicio (PaaS): nosotros solo subimos el código y Railway se encarga del servidor, el sistema operativo, la compilación, el HTTPS y el dominio público.

### Requisitos que debe cumplir el servidor

Se revisó el código de COP para saber qué necesita el servidor:

1. **Puerto 8080 fijo.** `Application.kt` escucha en `0.0.0.0:8080` y no lee ninguna variable de entorno. Como no se debe cambiar el backend, la plataforma tiene que adaptarse a ese puerto.
2. **Respuesta en menos de 10 segundos.** En el frontend, `ApiClient.kt` tiene `connectTimeoutMillis = 5_000` y `requestTimeoutMillis = 10_000`. Si el servidor tarda más de 10 s en contestar, la app marca error y el login falla.
3. **Ejecutar la JVM.** El backend es Ktor sobre Netty y corre en Java 21; ocupa unos 300 MB de RAM.
4. **Estar disponible todo el periodo** (de octubre a diciembre de 2026), sobre todo en las revisiones con el profesor.

### Comparación de plataformas

| Criterio | **Railway** | Render (gratis) | Koyeb (gratis) | Azure for Students |
|---|---|---|---|---|
| Puerto 8080 sin cambiar código | **Sí**, lo detecta solo | Sí, configurando `PORT=8080` | Sí, configurando el puerto | Sí, configurando el puerto |
| ¿Se duerme sin uso? | **No** | Sí, a los 15 min | Sí, a la hora (no se puede desactivar) | Depende del servicio elegido |
| Tiempo para despertar | No aplica | ~1 minuto | 1–5 s más el arranque de la JVM | Depende del servicio |
| ¿Cumple el límite de 10 s de la app? | **Sí** | **No**: el primer login falla | Dudoso: solo 0.1 vCPU para arrancar la JVM | Sí, con un plan que no se duerma |
| Recursos | 1 GB RAM en prueba; 0.5 GB en Free | No especificados | 512 MB RAM, 0.1 vCPU | Según el plan |
| Tarjeta de crédito | No para la prueba | No para el plan gratis | **Sí** (retención de $29 USD) | No (verificación con correo escolar) |
| Facilidad de configuración | **Alta**: detecta el `Dockerfile` | Alta | Media | Baja: portal, grupos de recursos, planes |
| Costo hasta diciembre | $5 USD de prueba (30 días) + Hobby $5 USD/mes | $0 | $0 | $0 con $100 USD de crédito (12 meses) |

### Decisión: Railway

- **Render queda descartado** aunque sea gratis: cuando el servidor está dormido tarda cerca de un minuto en responder y la app corta la petición a los 10 segundos. El primer login de cada sesión fallaría, que es justo lo que se ve en una revisión.
- **Koyeb queda descartado** porque se duerme sin opción de evitarlo, la JVM de Ktor arranca lento con 0.1 vCPU (riesgo de pasar los 10 s) y además pide tarjeta de crédito.
- **Azure for Students** cumple y sale gratis, pero su configuración es mucho más compleja y depende de que Microsoft acepte el correo institucional. Queda como **plan B** si no se puede pagar Railway.
- **Railway es la opción funcional:** se despliega **sin cambiar ni una línea del backend** (detecta el puerto 8080 y el `Dockerfile` solos), **no se duerme**, así que siempre contesta dentro de los 10 s que espera la app, y es la más sencilla de configurar y explicar. Cuando el proyecto pase de archivos JSON a una base de datos, Railway permite agregar MySQL en el mismo proyecto.

### Costo estimado

Railway cobra por uso: $10 USD por GB de RAM al mes y $20 USD por vCPU al mes. Con la JVM limitada a 300 MB y poco tráfico, el backend gasta aproximadamente **$3 USD al mes**.

| Periodo | Plan | Costo |
|---|---|---|
| Primeros 30 días | Prueba ($5 USD de crédito) | $0 |
| Resto del periodo hasta diciembre | Hobby (incluye $5 USD de uso) | $5 USD/mes, se puede dividir entre los 3 integrantes |

El plan Free de $1 USD al mes **no alcanza** para tener el servidor encendido todo el mes, por eso al terminar la prueba hay que pasar a Hobby.

### Por qué se usa Docker

El backend se compila con Gradle y corre en la JVM. El `Dockerfile` describe exactamente cómo hacerlo, así el servidor de Railway compila igual que nuestra PC:

1. **Etapa de compilación** (`azul/zulu-openjdk:21`): ejecuta `./gradlew installDist`, que genera el script de arranque `bin/COP-Backend` y las librerías en `build/install/COP-Backend`. Se usa JDK 21 de Azul porque es el que pide `gradle/gradle-daemon-jvm.properties`.
2. **Etapa de ejecución** (`azul/zulu-openjdk:21-jre`): solo copia el resultado compilado, por eso la imagen final es más ligera.
3. `chmod +x gradlew`: el proyecto se subió desde Windows y `gradlew` no tiene permiso de ejecución en Linux.
4. `JAVA_OPTS="-Xmx300m"`: limita la memoria de la JVM para que quepa en el plan gratuito (0.5 GB).

Los archivos `usuarios.json`, `servicios.json` y `citas.json` viajan **dentro del .jar** (están en `src/main/resources/data`), por lo que no hay que subirlos aparte.

---

## 2. Pasos para desplegar

1. Subir el proyecto `COP` a GitHub (con `COP-Backend/Dockerfile` y `COP-Backend/.dockerignore`).
2. Entrar a <https://railway.com> e iniciar sesión con GitHub. Verificar la cuenta de GitHub en <https://railway.com/verify> (sin verificar, la prueba tiene red y puertos limitados).
3. **New Project → Deploy from GitHub repo →** elegir el repositorio.
4. En el servicio: **Settings → Source → Root Directory** = `COP-Backend` (o `COP/COP-Backend`, según dónde quedó la carpeta en el repo). Así Railway solo compila el backend y no el frontend.
5. Esperar el build. En los logs debe aparecer `Using detected Dockerfile!` y al final `Responding at http://0.0.0.0:8080`.
6. **Settings → Networking → Generate Domain.** Revisar que el puerto destino sea **8080**.
7. Probar en el navegador: `https://<tu-app>.up.railway.app/` → `Servidor COP funcionando`.
8. Probar el login en Postman:

```
POST https://<tu-app>.up.railway.app/login
Content-Type: application/json

{"username": "ana", "password": "ana123"}
```

Rutas disponibles: `/`, `/login`, `/servicios`, `/profesionales`, `/citas`.

---

## 3. Conectar la app (COP-Frontend)

Esto es del **frontend**, no del backend. En:

```
COP-Frontend/shared/src/androidMain/kotlin/com/poligono/cop/data/remote/UrlServidor.android.kt
COP-Frontend/shared/src/iosMain/kotlin/com/poligono/cop/data/remote/UrlServidor.ios.kt
```

cambiar la dirección local por la de Railway:

```kotlin
actual fun urlServidor(): String = "https://<tu-app>.up.railway.app"
```

Ya no hace falta `adb reverse` ni tener el backend corriendo en la PC, y funciona en un celular físico con datos móviles.

---

## 4. A tener en cuenta

- **No activar la opción "Serverless" (app sleeping) del servicio en Railway:** haría que el servidor se durmiera y el primer login podría pasar de los 10 s que espera la app.
- **Cambiar al plan Hobby antes de que termine la prueba de 30 días**, para que el servidor no se detenga a mitad del periodo.
- Si se edita un `.json` de `resources/data`, hay que hacer push: Railway vuelve a compilar y desplegar solo.
- El contador de intentos fallidos de `AuthService` vive en memoria: se reinicia cada vez que Railway redespliega.
- Las contraseñas de `usuarios.json` están en texto plano dentro del repositorio; si el repo es público, cualquiera las puede ver. Para la entrega escolar funciona, pero en producción deberían ir en una base de datos con contraseñas cifradas.
