# SSH en el proyecto COP

El proyecto usa **SSH** en dos lugares, con **la misma llave**:

| Para qué | Con qué servidor | Comando de ejemplo |
|---|---|---|
| Subir y bajar el código y las ramas (control de versiones) | GitHub | `git push origin main` (remoto `git@github.com:...`) |
| **Desplegar el backend** (subir el programa e instalarlo) | Servidor Linux (Azure) | `scp` y `ssh azureuser@IP` dentro de `desplegar.ps1` |
| Administrar el servidor (estado, logs, reinicio) | Servidor Linux (Azure) | `ssh azureuser@IP` → `sudo systemctl status cop-backend` |

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

## 3. Servidor Linux por SSH (despliegue del backend)

1. Al crear la máquina virtual en Azure se elige **Clave pública SSH** y se pega la **misma** llave pública
   de la sección 1 (usuario `azureuser`). Así el servidor nunca usa contraseña.
2. Entrar:

   ```bash
   ssh azureuser@IP_DEL_SERVIDOR
   ```

3. Preparar el servidor (una vez) y desplegar (cada cambio), desde `COP-Backend`:

   ```powershell
   scp despliegue-ssh/preparar-servidor.sh azureuser@IP_DEL_SERVIDOR:/tmp/
   ssh azureuser@IP_DEL_SERVIDOR "sed -i 's/\r$//' /tmp/preparar-servidor.sh && sudo bash /tmp/preparar-servidor.sh"

   powershell -ExecutionPolicy Bypass -File despliegue-ssh\desplegar.ps1 azureuser@IP_DEL_SERVIDOR
   ```

Pasos completos y qué llenar en Azure, en `COP-Backend/DESPLIEGUE.md`.

**Si se usa Render:** Render despliega desde GitHub (no por SSH) y solo deja *entrar* por SSH en planes de pago
(`ssh srv-...@ssh.oregon.render.com`, ver `COP-Backend/DESPLIEGUE-RENDER.md`, sección 6).

---

## 4. Problemas comunes

| Mensaje | Qué hacer |
|---|---|
| `Permission denied (publickey)` | La llave pública no está en GitHub/Render o la PC manda otra. Revisa con `ssh -v ...` y `ssh-add -l`. |
| `Host key verification failed` | Escribe `yes` cuando pregunte por la huella del servidor. Si cambió, borra la línea con `ssh-keygen -R github.com`. |
| `ssh: connect to host ... port 22: Connection timed out` | La red de la escuela bloquea el puerto 22. Para GitHub usa el puerto 443 (abajo). |
| `Connection timed out` con el servidor de Azure | Revisa la IP pública y que la VM esté encendida; prueba con los datos del celular. |

GitHub por el puerto 443, si la red bloquea el 22. Crear el archivo `~/.ssh/config` con:

```
Host github.com
    HostName ssh.github.com
    Port 443
    User git
```
