#!/bin/bash
# =====================================================================
# Se ejecuta EN EL SERVIDOR (lo manda y lo corre desplegar.ps1 / desplegar.sh por SSH).
# Instala la versión nueva de COP-Backend que llegó por scp a /tmp y reinicia el servicio.
# =====================================================================
set -e

echo "Deteniendo la versión anterior..."
systemctl stop cop-backend 2>/dev/null || true

echo "Instalando la versión nueva en /opt/cop-backend..."
rm -rf /opt/cop-backend
mkdir -p /opt/cop-backend
tar -xzf /tmp/cop-backend.tar.gz -C /opt/cop-backend --strip-components=1
chmod +x /opt/cop-backend/bin/COP-Backend

echo "Instalando el servicio de systemd..."
sed 's/\r$//' /tmp/cop-backend.service > /etc/systemd/system/cop-backend.service
systemctl daemon-reload
systemctl enable cop-backend
systemctl start cop-backend

rm -f /tmp/cop-backend.tar.gz /tmp/cop-backend.service /tmp/instalar-en-servidor.sh

echo "Esperando a que arranque..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/ >/dev/null; then
        break
    fi
    sleep 2
done

systemctl --no-pager status cop-backend | head -n 5
echo
echo "Respuesta de http://localhost:8080/ :"
curl -s http://localhost:8080/
echo
