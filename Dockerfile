FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src src

RUN mvn -B clean package -DskipTests \
    && jar_file="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' -print -quit)" \
    && test -n "$jar_file" \
    && cp "$jar_file" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system lazydeploy \
    && useradd --system \
        --gid lazydeploy \
        --home-dir /app \
        --no-create-home \
        --shell /usr/sbin/nologin \
        lazydeploy

WORKDIR /app

COPY --from=build --chown=lazydeploy:lazydeploy /workspace/app.jar /app/app.jar

USER lazydeploy:lazydeploy

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
