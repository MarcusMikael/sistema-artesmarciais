# Etapa 1: construir a aplicação
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Dependências primeiro, numa camada própria: enquanto o pom.xml não mudar,
# o Docker reaproveita esta etapa e não baixa tudo de novo a cada commit.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# Etapa 2: executar a aplicação
FROM eclipse-temurin:21-jre

WORKDIR /app

# Roda sem privilégios de root.
RUN useradd --system --no-create-home --uid 10001 app

COPY --from=build --chown=app:app /app/target/ace-0.0.1-SNAPSHOT.jar app.jar

USER app

# Ajustes para contêiner pequeno (Render gratuito: 512 MB e pouca CPU):
# - usa até 75% da memória do contêiner para o heap;
# - coletor de lixo simples, adequado a uma CPU;
# - compilação mais leve, que encurta a inicialização;
# - encerra o processo em caso de falta de memória, para o Render reiniciar.
# Pode ser sobrescrito pela variável JAVA_TOOL_OPTIONS no Render.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
