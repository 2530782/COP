# =====================================================================
# Despliega COP-Backend en el servidor por SSH (PowerShell de Windows 10/11).
#
# Uso, desde la carpeta COP-Backend (Terminal de IntelliJ / Android Studio):
#   powershell -ExecutionPolicy Bypass -File despliegue-ssh\desplegar.ps1 azureuser@IP_DEL_SERVIDOR
#
# Pasos:
#   1. Compila en la PC con "gradlew installDist".
#   2. Empaqueta build\install\COP-Backend en un .tar.gz (tar viene con Windows 10/11).
#   3. Lo copia al servidor con scp (SSH).
#   4. Por SSH corre instalar-en-servidor.sh: lo descomprime en /opt/cop-backend,
#      instala el servicio de systemd y lo reinicia.
#   5. Comprueba que el servidor responda.
# =====================================================================
param(
    [Parameter(Mandatory = $true)]
    [string]$Servidor,

    [string]$Llave = "$HOME\.ssh\id_ed25519"
)

$ErrorActionPreference = "Stop"

Write-Host "== 1. Compilando COP-Backend"
.\gradlew.bat installDist
if ($LASTEXITCODE -ne 0) { throw "Falló la compilación" }

Write-Host "== 2. Empaquetando"
tar -czf build\cop-backend.tar.gz -C build\install COP-Backend
if ($LASTEXITCODE -ne 0) { throw "Falló el empaquetado" }

Write-Host "== 3. Copiando al servidor por SSH (scp)"
scp -i $Llave `
    build\cop-backend.tar.gz `
    despliegue-ssh\cop-backend.service `
    despliegue-ssh\instalar-en-servidor.sh `
    "${Servidor}:/tmp/"
if ($LASTEXITCODE -ne 0) { throw "Falló scp: revisa la IP, el usuario y la llave" }

Write-Host "== 4. Instalando y reiniciando el servicio (por SSH)"
# sed quita los saltos de línea de Windows (\r) antes de correr el script con bash
ssh -i $Llave $Servidor "sed -i 's/\r`$//' /tmp/instalar-en-servidor.sh && sudo bash /tmp/instalar-en-servidor.sh"
if ($LASTEXITCODE -ne 0) { throw "Falló la instalación en el servidor" }

$ip = $Servidor.Split("@")[-1]
Write-Host ""
Write-Host "Listo. Prueba en el navegador: http://${ip}:8080/  y  http://${ip}:8080/poligonos"
