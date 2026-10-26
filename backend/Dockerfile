# Etapa 1: compila o projeto com Maven e JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Baixa as dependências primeiro para aproveitar o cache do Docker quando só o código muda
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no CI antes do build da imagem
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final só com o JRE e o jar
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/orienta-saude-*.jar app.jar
USER app

EXPOSE 8080
# Usa até 75% da memória do contêiner para o heap
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
