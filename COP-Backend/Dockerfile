# Dockerfile para desplegar COP-Backend (Ktor Server) en Render (o Railway)
# Va dentro de la carpeta COP-Backend (junto a settings.gradle.kts).
# No modifica el código del backend: solo indica cómo compilarlo y ejecutarlo.

# ---------- Etapa 1: compilación ----------
# Azul Zulu 21 porque gradle/gradle-daemon-jvm.properties pide JDK 21 de AZUL
FROM azul/zulu-openjdk:21 AS build

WORKDIR /app

COPY . .

# chmod +x porque gradlew se subió desde Windows sin permiso de ejecución.
# installDist genera build/install/COP-Backend con el script de arranque y las librerías.
RUN chmod +x gradlew \
    && ./gradlew installDist --no-daemon

# ---------- Etapa 2: ejecución ----------
FROM azul/zulu-openjdk:21-jre

WORKDIR /app

# Para entrar al servidor por SSH (ssh srv-...@ssh.oregon.render.com):
# - Render pide que exista ~/.ssh con permisos 0700 (el contenedor corre como root).
# - curl sirve para probar el backend desde dentro: curl http://localhost:$PORT/
RUN mkdir -p /root/.ssh \
    && chmod 0700 /root/.ssh \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copia el servidor ya compilado (los JSON de resources/data van dentro del .jar
# y se copian a la base de datos la primera vez que arranca)
COPY --from=build /app/build/install/COP-Backend /app

# Script para revisar el servidor cuando se entra por SSH
COPY scripts /app/scripts

# Limita la memoria de la JVM para que quepa en el plan gratuito (0.5 GB)
ENV JAVA_OPTS="-Xmx300m"

# Application.kt escucha en 0.0.0.0:8080 o en el puerto de la variable PORT (Render)
EXPOSE 8080

CMD ["/app/bin/COP-Backend"]
