English | [Polski](README.docker.pl.md)

# Docker‑Compose Setup Guide

This guide walks you step by step through building and running the **PhotoFinder** infrastructure with *Docker Compose*.

## Prerequisites
- **Docker Engine ≥ 20.10** with the Docker Compose plugin (`docker compose`).
- **Java 21 (JDK)** – the project toolchain requires version 21 (see `build.gradle`); the `Dockerfile` builds the image on `eclipse-temurin:21-jdk-alpine`.
- **Gradle Wrapper** (`gradlew`) is in the project root; you don't need your own Gradle installation.
- The **`MONGO_PASSWORD`** environment variable – `spring.data.mongodb.password` in `application.properties` has no default value, so the `app` container won't start without it. Compose defaults the Mongo password to `example` (`MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD:-example}`) – set the same value for the application's environment.
- API keys for whichever AI providers you actually use (e.g. `OPENAI_API_KEY`, `MISTRAL_API_KEY`, `GROQ_API_KEY`…) – the full list of variables is in the `environment` section of the `app` service in `docker-compose.yml`.

## 1. Building the application image
```bash
# From the project root
./gradlew bootJar   # builds build/libs/*.jar (the Dockerfile copies it as app.jar)

docker compose build
```
`docker compose build` builds **only the `app` service image** – it's the only service with a `build:` section in `docker-compose.yml`. The other images (`rabbitmq:management`, `mongo:latest`, `qdrant/qdrant:latest`, `minio/minio:latest`, `grafana/grafana`, `prom/prometheus:latest`, `ollama/ollama:latest`, and the rest of the monitoring stack) are pulled ready-made from Docker Hub when you run `docker compose up`, not built locally. The `ollama-worker` module (a separate Gradle submodule, `./ollama-worker`) is not part of this Compose stack at all.

## 2. Starting the stack
```bash
docker compose --profile prod up -d
```
The `app` service has `profiles: [prod]` set in `docker-compose.yml` — plain `docker compose up -d` (without `--profile prod`) only starts the infrastructure (RabbitMQ, MongoDB, Qdrant, MinIO, Ollama, Prometheus, Grafana, Loki and the rest of the monitoring stack), **without the application itself**.

Containers will be reachable on the following ports:
- API + all actuator endpoints (health, prometheus) – `8081` (`server.port=8081`; a single shared port, there is no separate metrics port)
- RabbitMQ – `15672` (GUI), `5672` (AMQP), `15692` (Prometheus metrics)
- MongoDB – `27017`
- Qdrant – `6334` (gRPC); the REST/Web UI port is ephemeral (check the assigned host port with `docker compose port qdrant 6333`)
- MinIO – `9000` (API), `9001` (console)
- Grafana – `3050` (mapped to port `3000` inside the container)
- Prometheus – `9090`
- Zipkin – `9411`, Loki – `3100`

## 3. Checking the stack's health
```bash
docker compose ps
```

To verify the application is running:
- `GET http://localhost:8081/actuator/health` → `UP`
- `GET http://localhost:8081/actuator/prometheus` → Prometheus data
- `http://localhost:15672` – RabbitMQ management UI in the browser (login `admin`/`admin`, see `RABBITMQ_DEFAULT_USER`/`RABBITMQ_DEFAULT_PASS` in `docker-compose.yml`)
- Grafana at `http://localhost:3050` – login `admin`/`example` (see `GF_SECURITY_ADMIN_PASSWORD` in `docker-compose.yml`)

## 4. Running the tests
```bash
./gradlew test
```
Runs all unit tests **and** the tests under the `.../integration/` package — the latter mock the infrastructure layer (Mockito + `MockMvc`) and don't require any containers to be running. There is no separate Gradle task in this repository for tests that need a live Docker environment.

## 5. Tearing the environment down
```bash
docker compose --profile prod down -v
```
The `--profile prod` flag ensures the `app` container is also stopped and removed if it was running; `-v` removes the volumes so no data is left behind.

---
> **Tip** – a `docker-compose.override.yml` file is not yet part of the repository; you can create one yourself in a development setup to override environment variables or ports without touching the main `docker-compose.yml` (Compose merges both files automatically once an override file is present).
