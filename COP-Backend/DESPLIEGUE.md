# Despliegue de COP-Backend por SSH

El backend se despliega en un **servidor Linux remoto** usando solo **SSH**:

- `ssh` para entrar al servidor y ejecutar comandos.
- `scp` (copia por SSH) para subir el programa compilado.

No se usa contraseña: se entra con una **llave SSH** (cómo crearla está en `SSH.md`, en la raíz del repositorio).

```
 PC (Windows)                                      Servidor Linux (Ubuntu)
 ┌───────────────────────────┐                     ┌────────────────────────────────────┐
 │ gradlew installDist       │                     │ /opt/cop-backend      (programa)   │
 │   → build/install/        │  scp  (puerto 22)   │ /var/lib/cop-backend  (base H2)    │
 │ cop-backend.tar.gz ───────┼────────────────────▶│ /etc/cop-backend.env  (PORT, JWT)  │
 │                           │  ssh  (puerto 22)   │ systemd: cop-backend.service       │
 │ instalar-en-servidor.sh ──┼────────────────────▶│   escucha en 0.0.0.0:8080          │
 └───────────────────────────┘                     └──────────────┬─────────────────────┘
                                                                  │ HTTP :8080
                                                    App COP (teléfono)
```

## Archivos (carpeta `despliegue-ssh/`)

| Archivo | Dónde corre | Qué hace |
|---|---|---|
| `preparar-servidor.sh` | Servidor, **una sola vez** | Instala Java 21 (JRE), crea el usuario `cop`, las carpetas, la clave JWT, 1 GB de swap y abre los puertos 22 y 8080 |
| `desplegar.ps1` | PC con Windows (PowerShell) | Compila, empaqueta, sube con `scp` y ejecuta la instalación por `ssh` |
| `desplegar.sh` | PC con Git Bash, Linux o macOS | Lo mismo que `desplegar.ps1` |
| `instalar-en-servidor.sh` | Servidor (lo manda `desplegar`) | Detiene la versión anterior, instala la nueva en `/opt/cop-backend` y reinicia el servicio |
| `cop-backend.service` | Servidor (systemd) | Mantiene el backend corriendo: arranca al prender el servidor y lo levanta solo si se cae |

## 1. Conseguir el servidor (Azure for Students)

Cualquier servidor Ubuntu con IP pública sirve: Azure, AWS, Google Cloud, DigitalOcean, Oracle o uno de la escuela.
Aquí se usa **Azure for Students**: 100 USD de crédito por 12 meses, **sin tarjeta**, con el correo de la universidad.
Incluye 750 horas al mes de una máquina **B1s** durante 12 meses.

1. Entrar a <https://azure.microsoft.com/free/students> con el correo `@upt.edu.mx`.
2. Portal de Azure → **Máquinas virtuales → Crear → Máquina virtual de Azure**.
3. Llenar:
   - **Grupo de recursos:** nuevo, `cop`.
   - **Nombre:** `cop-servidor`.
   - **Imagen:** *Ubuntu Server 24.04 LTS – x64*.
   - **Tamaño:** *Standard_B1s* (si sale "Apto para servicios gratuitos", mejor).
   - **Tipo de autenticación:** **Clave pública SSH**.
   - **Nombre de usuario:** `azureuser`.
   - **Origen de la clave pública SSH:** *Usar una clave pública existente* → pegar el contenido de `~/.ssh/id_ed25519.pub`.
   - **Puertos de entrada públicos:** permitir **SSH (22)**.
4. **Revisar y crear → Crear**. Tarda 1 o 2 minutos.
5. En la máquina → **Información general**, copiar la **Dirección IP pública** (por ejemplo `20.120.33.45`).
6. **Abrir el puerto de la API:** máquina → **Redes → Configuración de red → Crear regla de puerto → Regla de puerto de entrada**:
   puerto de destino `8080`, protocolo TCP, acción Permitir, nombre `cop-api`.

## 2. Primera conexión por SSH

Desde PowerShell (o la Terminal de IntelliJ / Android Studio):

```powershell
ssh azureuser@20.120.33.45
```

La primera vez pregunta si confías en el servidor: escribe `yes`. Si entras, verás algo como
`azureuser@cop-servidor:~$`. Sal con `exit`.

## 3. Preparar el servidor (una sola vez)

Desde la carpeta `COP-Backend`:

```powershell
scp despliegue-ssh/preparar-servidor.sh azureuser@20.120.33.45:/tmp/
ssh azureuser@20.120.33.45 "sed -i 's/\r$//' /tmp/preparar-servidor.sh && sudo bash /tmp/preparar-servidor.sh"
```

El `sed` quita los saltos de línea de Windows (`\r`) para que bash no falle.
Al final dice `Servidor listo`.

## 4. Desplegar (cada vez que cambie el backend)

Desde la carpeta `COP-Backend`:

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File despliegue-ssh\desplegar.ps1 azureuser@20.120.33.45
```

**Git Bash, Linux o macOS:**

```bash
bash despliegue-ssh/desplegar.sh azureuser@20.120.33.45
```

El script hace todo:

1. `gradlew installDist` → compila en la PC (el servidor pequeño no tiene que compilar).
2. `tar` → empaqueta `build/install/COP-Backend` en `build/cop-backend.tar.gz`.
3. `scp` → sube el paquete, `cop-backend.service` e `instalar-en-servidor.sh` a `/tmp` del servidor.
4. `ssh` → corre `instalar-en-servidor.sh` con `sudo`: detiene la versión anterior, la reemplaza en
   `/opt/cop-backend`, instala el servicio de systemd y lo arranca.
5. Espera a que el backend conteste y muestra `Servidor COP funcionando`.

Si la llave no está en `~/.ssh/id_ed25519`, pásala con `-Llave C:\ruta\llave` (PowerShell)
o `LLAVE_SSH=/ruta/llave bash despliegue-ssh/desplegar.sh ...`.

## 5. Probar

- Navegador: `http://20.120.33.45:8080/` → `Servidor COP funcionando`
- Navegador: `http://20.120.33.45:8080/poligonos` → JSON con los polígonos
- Postman:

  ```
  POST http://20.120.33.45:8080/login
  Content-Type: application/json

  {"username": "ana", "password": "ana123"}
  ```

## 6. Conectar la app

En `COP-Frontend/shared/src/androidMain/kotlin/com/poligono/cop/data/remote/UrlServidor.android.kt`:

```kotlin
const val USAR_SERVIDOR_REMOTO = true
const val URL_SERVIDOR_REMOTO = "http://20.120.33.45:8080"   // la IP pública del servidor
```

La app ya permite HTTP sin cifrar (`android:usesCleartextTraffic="true"` en el `AndroidManifest.xml`),
así que funciona con la IP directa. Ya no hace falta `adb reverse` y la app funciona en un celular físico con datos móviles.

## 7. Administrar el servidor por SSH

```bash
ssh azureuser@20.120.33.45

sudo systemctl status cop-backend      # ¿está corriendo? (active = sí)
sudo systemctl restart cop-backend     # reiniciar
sudo journalctl -u cop-backend -f      # log en vivo, Ctrl+C para salir
sudo journalctl -u cop-backend -n 50   # últimas 50 líneas del log
curl http://localhost:8080/            # probar desde dentro del servidor
ls /opt/cop-backend                    # bin/ y lib/ (los .jar)
ls /var/lib/cop-backend/datos          # cop.mv.db (base de datos H2)
free -h                                # memoria
exit
```

**Respaldo de la base de datos** (desde la PC):

```powershell
scp azureuser@20.120.33.45:/var/lib/cop-backend/datos/cop.mv.db .
```

## 8. Base de datos

En el servidor se usa **H2 en archivo** (`/var/lib/cop-backend/datos/cop.mv.db`), el mismo modo que en la PC:
los datos **no se pierden** al reiniciar o volver a desplegar, porque `instalar-en-servidor.sh` solo reemplaza
`/opt/cop-backend`. La primera vez el backend crea las tablas y carga los datos de prueba (`Esquema.kt`).

Para usar PostgreSQL en el mismo servidor: `sudo apt install postgresql`, crear la base y agregar en
`/etc/cop-backend.env` la línea `DATABASE_URL=postgresql://usuario:clave@localhost:5432/cop`
(`BaseDatos.kt` ya la entiende). Después, `sudo systemctl restart cop-backend`.

## 9. Problemas comunes

| Qué pasa | Qué hacer |
|---|---|
| `Permission denied (publickey)` | La llave que pegaste en Azure no es la de tu PC. Prueba con `ssh -i ~/.ssh/id_ed25519 -v azureuser@IP`. |
| `Connection timed out` en el puerto 22 | La red de la escuela bloquea el puerto 22. Usa los datos del celular o pide que lo abran. |
| El navegador no abre `http://IP:8080/` | Falta la regla del puerto 8080 en Azure (paso 1.6) o el servicio no está corriendo (`systemctl status`). |
| `gradlew` falla en el paso 1 | Compila primero desde IntelliJ / Android Studio para que descargue Gradle y el JDK 21. |
| El servicio se reinicia solo | Revisa el error con `sudo journalctl -u cop-backend -n 50`. |
| Se acaba el crédito o se apaga la VM | En Azure, **Detener** la máquina cuando no se use ahorra crédito (la IP puede cambiar si no es estática). |

## Alternativa: Render

`DESPLIEGUE-RENDER.md` explica el despliegue en Render (PaaS) con `render.yaml` y Docker.
Render despliega desde GitHub y no por SSH; solo permite **entrar** por SSH en planes de pago.
