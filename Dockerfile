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

# Ajustes para contêiner pequeno (Render gratuito: 512 MB e pouca CPU):
# - usa até 75% da memória do contêiner para o heap;
# - coletor de lixo simples, adequado a uma CPU;
# - compilação mais leve, que encurta a inicialização;
# - encerra o processo em caso de falta de memória, para o Render reiniciar.
# Pode ser sobrescrito pela variável JAVA_TOOL_OPTIONS no Render.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

COPY --from=build /app/target/ace-0.0.1-SNAPSHOT.jar app.jar

# ---------- Inicialização rápida (o Render gratuito desliga o serviço sem uso) ----------
# 1. Extrai o jar: carregar classes de uma pasta é mais rápido que de um jar dentro de outro jar.
# 2. Treino de CDS (Class Data Sharing): sobe o contexto do Spring uma vez durante o build e grava as
#    classes carregadas em application.jsa; nas próximas partidas a JVM as lê direto desse arquivo.
#    O treino usa H2 em memória (não há banco no build), uma senha descartável e encerra assim que o
#    contexto fica pronto. Nada disso vale em produção, que continua exigindo DB_URL e ADMIN_PASSWORD.
RUN java -Djarmode=tools -jar app.jar extract --destination application \
    && rm app.jar \
    && java -XX:ArchiveClassesAtExit=application/application.jsa \
        -Dspring.context.exit=onRefresh \
        -Dspring.datasource.url="jdbc:h2:mem:cds;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE" \
        -Dspring.datasource.driver-class-name=org.h2.Driver \
        -Dspring.datasource.username=sa -Dspring.datasource.password= \
        -Dapp.admin.password=apenas-treino-cds-durante-o-build \
        -Dserver.port=0 \
        -jar application/app.jar

# Roda sem privilégios de root.
RUN useradd --system --no-create-home --uid 10001 app \
    && chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:SharedArchiveFile=application/application.jsa", "-jar", "application/app.jar"]
