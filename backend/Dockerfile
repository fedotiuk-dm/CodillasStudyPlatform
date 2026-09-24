# Build context is the repository root. Build the whole multi-module backend, then run the `main`
# boot jar. Liberica JDK 25 (musl/Alpine).
FROM bellsoft/liberica-runtime-container:jdk-25-musl AS build
WORKDIR /workspace
COPY backend/ ./
RUN ./mvnw -q -B -DskipTests -pl main -am package

FROM bellsoft/liberica-runtime-container:jdk-25-musl
WORKDIR /app
COPY --from=build /workspace/main/target/codillas-study-platform.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
