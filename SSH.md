# SSH en el proyecto COP

El proyecto usa **SSH** en dos lugares, con **la misma llave**:

| Para qué | Con qué servidor | Comando de ejemplo |
|---|---|---|
| Subir y bajar el código y las ramas (control de versiones) | GitHub | `git push origin main` (remoto `git@github.com:...`) |
| Entrar al servidor remoto donde corre el backend | Render | `ssh srv-xxxxxxxxxxxx@ssh.oregon.render.com` |

**Qué es SSH:** un protocolo para conectarse a otra computadora de forma cifrada.
En lugar de contraseña se usa un **par de llaves**:

- **Llave privada** (`~/.ssh/id_ed25519`): se queda solo en tu PC. **Nunca se comparte ni se sube al repositorio.**
- **Llave pública** (`~/.ssh/id_ed25519.pub`): se pega en GitHub y en Render. Con ella el servidor comprueba
  que quien se conecta tiene la llave privada correspondiente.

Los comandos funcionan igual en **PowerShell de Windows 10/11**, Git Bash, macOS y Linux.
En Windows, `~` es `C:\Users\TU_USUARIO`.

---

## 1. Crear la llave (una sola vez por PC)

```bash
ssh-keygen -t ed25519 -C "2530782@upt.edu.mx"
```

- Enter para aceptar la ruta (`~/.ssh/id_ed25519`).
- Escribe una frase de paso o deja vacío con Enter.

Ver la llave pública para copiarla:

```bash
cat ~/.ssh/id_ed25519.pub
```

Sale una sola línea que empieza con `ssh-ed25519 AAAA...`. Esa es la que se copia.

---

## 2. GitHub por SSH (repositorio y ramas)

### 2.1 Agregar la llave a GitHub

GitHub → foto de perfil → **Settings → SSH and GPG keys → New SSH key**.
Title: `PC-escuela` (o el nombre que quieras), Key type: *Authentication Key*, pegar la línea y **Add SSH key**.

Probar la conexión:

```bash
ssh -T git@github.com
```

La primera vez pregunta por la huella del servidor: escribe `yes`.
Debe responder `Hi TU_USUARIO! You've successfully authenticated...`.

### 2.2 Subir el proyecto con sus ramas

Crear en GitHub un repositorio vacío (sin README), por ejemplo `COP`, y desde la carpeta `COP`:

```bash
git remote add origin git@github.com:TU_USUARIO/COP.git
git push -u origin main
git push -u origin backend
git push -u origin frontend
```

Si el repositorio ya tenía un remoto con `https://`, se cambia a SSH con:

```bash
git remote set-url origin git@github.com:TU_USUARIO/COP.git
git remote -v
```

### 2.3 Ramas del repositorio

| Rama | Qué lleva |
|---|---|
| `main` | Versión completa e integrada; Render despliega desde aquí |
| `backend` | Cambios del servidor: `COP-Backend/`, `Dockerfile`, `render.yaml` |
| `frontend` | Cambios de la app: `COP-Frontend/` (pantallas, huella, GPS) |

Forma de trabajar: cada cambio se hace en su rama y luego se une a `main`.

```bash
git checkout backend
# ... cambios en COP-Backend ...
git add .
git commit -m "COP-Backend: descripción del cambio"
git push

git checkout main
git merge --no-ff backend
git push
```

Para enseñar los cambios por rama en el examen:

```bash
git log --oneline --graph --all
```

En GitHub: pestaña **Insights → Network**, o el selector de ramas arriba de los archivos.

---

## 3. Render por SSH (servidor remoto del backend)

Render **no da SSH en el plan Free** de servicios web, solo en los de pago. Para la revisión,
cambia `cop-backend` a **Starter** (7 USD al mes, se cobra proporcional al segundo):
*cop-backend → Settings → Instance Type → Starter*. Al terminar regrésalo a **Free**.

1. Render → tu foto → **Account Settings → SSH Public Keys → + Add SSH Public Key**
   → pegar la **misma** llave pública de la sección 1.
2. Servicio `cop-backend` → botón **Connect → SSH** → copiar el comando:

   ```bash
   ssh srv-xxxxxxxxxxxx@ssh.oregon.render.com
   ```

3. Ya dentro del servidor:

   ```bash
   sh /app/scripts/revisar-servidor.sh   # proceso, variables y prueba de /, /poligonos y /login
   ls /app                               # bin/, lib/, scripts/
   echo $PORT                            # puerto asignado por Render
   curl http://localhost:$PORT/
   exit
   ```

Más detalle en `COP-Backend/DESPLIEGUE.md`, sección 6.

---

## 4. Problemas comunes

| Mensaje | Qué hacer |
|---|---|
| `Permission denied (publickey)` | La llave pública no está en GitHub/Render o la PC manda otra. Revisa con `ssh -v ...` y `ssh-add -l`. |
| `Host key verification failed` | Escribe `yes` cuando pregunte por la huella del servidor. Si cambió, borra la línea con `ssh-keygen -R github.com`. |
| `ssh: connect to host ... port 22: Connection timed out` | La red de la escuela bloquea el puerto 22. Para GitHub usa el puerto 443 (abajo). |
| En Render no aparece la opción SSH | El servicio está en el plan Free; cámbialo a Starter. |

GitHub por el puerto 443, si la red bloquea el 22. Crear el archivo `~/.ssh/config` con:

```
Host github.com
    HostName ssh.github.com
    Port 443
    User git
```
