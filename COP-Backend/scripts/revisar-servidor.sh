#!/bin/sh
# Revisión rápida del backend de COP desde DENTRO del servidor de Render.
# Uso (ya conectado por SSH):  sh /app/scripts/revisar-servidor.sh

PUERTO="${PORT:-8080}"

echo "== Servidor"
echo "Equipo:  $(hostname)"
echo "Sistema: $(uname -sr)"
echo "Java:    $(java -version 2>&1 | head -n 1)"
echo

echo "== Proceso del backend"
ps -eo pid,etime,rss,args | grep "[C]OP-Backend" || echo "No se encontró el proceso"
echo

echo "== Variables (sin mostrar secretos)"
echo "PORT=$PUERTO"
if [ -n "$DATABASE_URL" ]; then
    echo "DATABASE_URL=definida (PostgreSQL)"
else
    echo "DATABASE_URL=no definida (se usa H2 en archivo)"
fi
if [ -n "$JWT_SECRETO" ]; then
    echo "JWT_SECRETO=definida"
else
    echo "JWT_SECRETO=no definida (se usa la de desarrollo)"
fi
echo

echo "== Prueba de endpoints"
echo "GET /"
curl -s "http://localhost:$PUERTO/"
echo
echo
echo "GET /poligonos"
curl -s "http://localhost:$PUERTO/poligonos" | head -c 300
echo
echo
echo "POST /login (ana / ana123)"
curl -s -X POST "http://localhost:$PUERTO/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ana","password":"ana123"}' | head -c 200
echo
