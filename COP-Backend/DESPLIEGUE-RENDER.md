# Alternativa: despliegue de COP-Backend en Render (PaaS)

> El despliegue principal es por SSH a un servidor Linux: ver `DESPLIEGUE.md`.

Por indicación del profesor el backend se aloja en **Render** (Railway daba problemas con las cuentas).
Render es una Plataforma como Servicio (PaaS): se conecta el repositorio de GitHub y Render
compila el `Dockerfile`, da HTTPS y un dominio público `https://<nombre>.onrender.com`.

## 1. Qué necesita el servidor (y cómo ya lo cumple el backend)

| Necesidad | Dónde se resuelve |
|---|---|
| Escuchar en el puerto que asigna Render | `Configuracion.kt` lee la variable `PORT` (en la PC usa 8080) |
| Guardar los datos aunque el servidor se reinicie | `BaseDatos.kt` usa PostgreSQL si existe `DATABASE_URL`; si no, H2 en archivo |
| Clave secreta del token JWT | Variable `JWT_SECRETO` (Render la genera sola con `render.yaml`) |
| Compilar y correr la JVM (Java 21) | `Dockerfile` en dos etapas (compila con Gradle, ejecuta con JRE) |
| Aguantar que el plan gratis "despierte" (≈ 1 min) | En la app, `ApiClient.kt` espera hasta 60 s |

## 2. Archivos que usa Render

- `render.yaml` (en la **raíz del repositorio**, junto a `COP-Backend/` y `COP-Frontend/`): es el
  *Blueprint*. Crea el servicio web `cop-backend` (Docker, plan free) y la base `cop-db` (PostgreSQL, plan free)
  y conecta las dos con `DATABASE_URL`.
- `COP-Backend/Dockerfile` y `COP-Backend/.dockerignore`.
- Solo se compila la carpeta `COP-Backend` (`dockerContext: ./COP-Backend`), el frontend no se toca.

## 3. Pasos

1. Subir el repositorio a GitHub con las ramas `main`, `backend` y `frontend` (ver `README.md` de la raíz).
2. Entrar a <https://dashboard.render.com> con la cuenta de GitHub.
3. **New → Blueprint →** elegir el repositorio, rama `main`. Render lee `render.yaml` y muestra
   `cop-backend` y `cop-db`. Dar **Apply**.
4. Esperar el build (5–10 min la primera vez). En **Logs** debe aparecer `Responding at http://0.0.0.0:...`.
5. Copiar la dirección del servicio (arriba a la izquierda), por ejemplo `https://cop-backend.onrender.com`.
6. Probar en el navegador:
   - `https://cop-backend.onrender.com/` → `Servidor COP funcionando`
   - `https://cop-backend.onrender.com/poligonos` → lista de polígonos en JSON
7. Probar el login en Postman:

```
POST https://cop-backend.onrender.com/login
Content-Type: application/json

{"username": "ana", "password": "ana123"}
```

**Sin Blueprint (manual):** New → Web Service → repositorio → Language **Docker**,
Root Directory `COP-Backend`, Instance type **Free**; en Environment agregar `JWT_SECRETO` (cualquier texto largo)
y, si se creó una base PostgreSQL, `DATABASE_URL` = su *Internal Database URL*.

## 4. Conectar la app

En `COP-Frontend/shared/src/androidMain/kotlin/com/poligono/cop/data/remote/UrlServidor.android.kt`:

```kotlin
const val USAR_SERVIDOR_REMOTO = true
const val URL_RENDER = "https://cop-backend.onrender.com"   // la dirección del paso 5
```

Con `USAR_SERVIDOR_REMOTO = true` ya no hace falta `adb reverse` ni tener el backend en la PC,
y la app funciona en un celular físico con datos móviles.

## 5. A tener en cuenta

- **El plan gratis se duerme a los 15 min sin uso.** La primera petición tarda cerca de un minuto
  (la app espera 60 s). **Antes de la revisión abre la URL en el navegador** para despertarlo.
- **La base PostgreSQL gratis de Render vence a los 30 días** de creada. Créala cerca de la
  presentación (2–7 de diciembre) o vuelve a crearla; al arrancar, el backend crea las tablas y
  carga los datos de prueba solo (`Esquema.kt`).
- Cada `git push` a `main` vuelve a compilar y desplegar automáticamente.
- Las contraseñas se guardan cifradas (PBKDF2, `Contrasenas.kt`), aunque los JSON de datos iniciales
  del repositorio tengan las contraseñas de prueba.

## 6. Entrar al servidor de Render por SSH

Render deja entrar por **SSH** al contenedor donde corre el backend. Así se comprueba en vivo
que el servidor remoto está funcionando: el proceso de Java, las variables y las respuestas de la API.
Los pasos para crear la llave están en `SSH.md` (raíz), en la raíz del repositorio.

**Importante:** Render **no da SSH en el plan Free** de servicios web, solo en los de pago.
Para la revisión se puede cambiar `cop-backend` a la instancia de pago más barata
(**Starter, 7 USD al mes, se cobra proporcional al segundo**) y regresarlo a Free al terminar:
*Settings → Instance Type → Starter → Save*. La base `cop-db` se puede quedar en Free.

1. **Agregar la llave pública a Render:** *Account Settings → SSH Public Keys → + Add SSH Public Key*,
   y pegar el contenido de `~/.ssh/id_ed25519.pub`. Es la misma llave que se usa en GitHub.
2. **Copiar el comando:** en el servicio `cop-backend`, pestaña **Connect → SSH**. Se ve así:

   ```bash
   ssh srv-xxxxxxxxxxxx@ssh.oregon.render.com
   ```

   `srv-...` es el ID del servicio y `oregon` la región donde se creó (`ohio`, `virginia`, `frankfurt` o `singapore`).
3. **Conectarse** desde la terminal de la PC. La primera vez se escribe `yes` para aceptar la huella del servidor.
4. **Ya dentro, revisar el backend** con el script que trae la imagen de Docker:

   ```bash
   sh /app/scripts/revisar-servidor.sh
   ```

   Muestra el sistema y la versión de Java, el proceso `COP-Backend`, si hay `DATABASE_URL` y `JWT_SECRETO`
   (sin enseñar sus valores) y prueba `GET /`, `GET /poligonos` y `POST /login` dentro del servidor.
5. Otros comandos útiles dentro del servidor:

   ```bash
   ls /app            # bin/ (script de arranque), lib/ (los .jar) y scripts/
   ls /app/lib        # librerías: ktor, netty, postgresql, h2, openpdf...
   echo $PORT         # puerto que Render asignó
   curl http://localhost:$PORT/poligonos
   exit               # salir
   ```

Para que el SSH funcione, el `Dockerfile` crea `/root/.ssh` con permisos `0700` (Render lo pide)
e instala `curl`. La imagen no trae su propio servidor SSH: lo pone Render.
Si sale `Permission denied (publickey)`, revisa con `ssh -v srv-...@ssh.oregon.render.com` qué llave se está mandando
y que sea la misma que se agregó en Render.
Cada redeploy cierra las sesiones de SSH abiertas.
