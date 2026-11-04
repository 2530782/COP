#!/bin/bash
# =====================================================================
# Prepara un servidor Linux (Ubuntu 22.04 / 24.04) para correr COP-Backend.
# Se corre UNA sola vez, desde la PC, a través de SSH:
#
#   scp despliegue-ssh/preparar-servidor.sh azureuser@IP_DEL_SERVIDOR:/tmp/
#   ssh azureuser@IP_DEL_SERVIDOR "sed -i 's/\r$//' /tmp/preparar-servidor.sh && sudo bash /tmp/preparar-servidor.sh"
#
# Qué hace:
#   1. Instala Java 21 (solo el JRE: el backend se compila en la PC).
#   2. Crea el usuario del sistema "cop" (el backend no corre como root).
#   3. Crea las carpetas /opt/cop-backend (programa) y /var/lib/cop-backend (base de datos H2).
#   4. Genera la clave secreta del token JWT en /etc/cop-backend.env.
#   5. Crea un archivo swap de 1 GB (las máquinas pequeñas tienen 1 GB de RAM).
#   6. Abre los puertos 22 (SSH) y 8080 (API) en el firewall ufw.
# =====================================================================
set -e

echo "== 1. Java 21"
apt-get update -y
apt-get install -y openjdk-21-jre-headless curl
java -version

echo "== 2. Usuario del sistema 'cop'"
if ! id cop >/dev/null 2>&1; then
    useradd --system --home /var/lib/cop-backend --shell /usr/sbin/nologin cop
fi

echo "== 3. Carpetas"
mkdir -p /opt/cop-backend
mkdir -p /var/lib/cop-backend
chown -R cop:cop /var/lib/cop-backend

echo "== 4. Variables del servicio"
if [ ! -f /etc/cop-backend.env ]; then
    SECRETO=$(head -c 48 /dev/urandom | base64 | tr -d '\n/+=')
    cat > /etc/cop-backend.env <<EOF
PORT=8080
JWT_SECRETO=$SECRETO
JAVA_OPTS=-Xmx300m
EOF
    chmod 600 /etc/cop-backend.env
fi

echo "== 5. Swap de 1 GB"
if [ ! -f /swapfile ]; then
    fallocate -l 1G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    if swapon /swapfile; then
        echo "/swapfile none swap sw 0 0" >> /etc/fstab
    else
        echo "No se pudo activar el swap (se continúa sin él)"
        rm -f /swapfile
    fi
fi

echo "== 6. Firewall"
if command -v ufw >/dev/null 2>&1; then
    ufw allow OpenSSH
    ufw allow 8080/tcp
    ufw --force enable
fi

echo
echo "Servidor listo. Ahora, desde la PC, corre desplegar.ps1 (Windows) o desplegar.sh."
