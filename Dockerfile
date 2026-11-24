# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy@sha256:96ac8197b2cbc2bb0f9fbcbf91dbb0015f43b3c76a1cb351892da12e7bb91bed AS build
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy@sha256:c6a5dd70fb24f4e07ef88cbda8efb613a5a649a1af071a41de8eaabd1ee6b5da
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/foro-contenido-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chmod=755 docker/healthcheck.sh /app/healthcheck.sh
RUN sed -i 's/\r$//' /app/healthcheck.sh
ENV PORT=8082 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=10.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=6s --start-period=120s --retries=5 CMD ["/app/healthcheck.sh"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
