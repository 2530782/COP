#!/bin/bash
# =====================================================================
# Despliega COP-Backend en el servidor por SSH (Git Bash, Linux o macOS).
#
# Uso, desde la carpeta COP-Backend:
#   bash despliegue-ssh/desplegar.sh azureuser@IP_DEL_SERVIDOR
#
# Pasos:
#   1. Compila en la PC con "gradlew installDist".
#   2. Empaqueta build/install/COP-Backend en un .tar.gz.
#   3. Lo copia al servidor con scp (SSH).
#   4. Por SSH corre instalar-en-servidor.sh: lo descomprime en /opt/cop-backend,
#      instala el servicio de systemd y lo reinicia.
#   5. Comprueba que el servidor responda.
# =====================================================================
set -e

SERVIDOR="$1"
LLAVE="${LLAVE_SSH:-$HOME/.ssh/id_ed25519}"

if [ -z "$SERVIDOR" ]; then
    echo "Uso: bash despliegue-ssh/desplegar.sh usuario@IP_DEL_SERVIDOR"
    exit 1
fi

echo "== 1. Compilando COP-Backend"
chmod +x gradlew
./gradlew installDist

echo "== 2. Empaquetando"
tar -czf build/cop-backend.tar.gz -C build/install COP-Backend

echo "== 3. Copiando al servidor por SSH (scp)"
scp -i "$LLAVE" \
    build/cop-backend.tar.gz \
    despliegue-ssh/cop-backend.service \
    despliegue-ssh/instalar-en-servidor.sh \
    "$SERVIDOR":/tmp/

echo "== 4. Instalando y reiniciando el servicio (por SSH)"
ssh -i "$LLAVE" "$SERVIDOR" "sed -i 's/\r\$//' /tmp/instalar-en-servidor.sh && sudo bash /tmp/instalar-en-servidor.sh"

IP="${SERVIDOR#*@}"
echo
echo "Listo. Prueba en el navegador: http://$IP:8080/  y  http://$IP:8080/poligonos"
