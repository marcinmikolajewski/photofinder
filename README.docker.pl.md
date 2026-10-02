[English](README.docker.md) | Polski

# Instalacja i konfiguracja Docker‑Compose

Ten przewodnik opisuje krok po kroku jak zbudować oraz uruchomić infrastrukturę **PhotoFinder** przy pomocy *Docker Compose*.

## Wymagania wstępne
- **Docker Engine ≥ 20.10** z wtyczką Docker Compose (`docker compose`).
- **Java 21 (JDK)** – toolchain projektu wymaga wersji 21 (patrz `build.gradle`); `Dockerfile` buduje obraz na `eclipse-temurin:21-jdk-alpine`.
- **Gradle Wrapper** (`gradlew`) znajduje się w głównym katalogu; nie potrzebujesz własnej instalacji Gradle.
- Zmienna środowiskowa **`MONGO_PASSWORD`** – `spring.data.mongodb.password` w `application.properties` nie ma wartości domyślnej, więc bez niej kontener `app` nie wystartuje. Compose domyślnie ustawia hasło Mongo na `example` (`MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD:-example}`) – ustaw tę samą wartość w środowisku aplikacji.
- Klucze API dostawców AI, których faktycznie używasz (np. `OPENAI_API_KEY`, `MISTRAL_API_KEY`, `GROQ_API_KEY`…) – pełna lista zmiennych jest w sekcji `environment` usługi `app` w `docker-compose.yml`.

## 1. Budowa obrazu aplikacji
```bash
# Z poziomu folderu projektu
./gradlew bootJar   # buduje build/libs/*.jar (Dockerfile kopiuje go jako app.jar)

docker compose build
```
`docker compose build` buduje **tylko obraz usługi `app`** – to jedyna usługa z sekcją `build:` w `docker-compose.yml`. Pozostałe obrazy (`rabbitmq:management`, `mongo:latest`, `qdrant/qdrant:latest`, `minio/minio:latest`, `grafana/grafana`, `prom/prometheus:latest`, `ollama/ollama:latest` i reszta stosu monitoringu) są pobierane gotowe z Docker Hub przy `docker compose up`, a nie budowane lokalnie. Moduł `ollama-worker` (osobny submoduł Gradle, `./ollama-worker`) nie jest częścią tego stacku Compose.

## 2. Uruchamianie stacku
```bash
docker compose --profile prod up -d
```
Usługa `app` ma w `docker-compose.yml` ustawione `profiles: [prod]` — samo `docker compose up -d` (bez `--profile prod`) uruchomi tylko infrastrukturę (RabbitMQ, MongoDB, Qdrant, MinIO, Ollama, Prometheus, Grafana, Loki i resztę monitoringu), **bez samej aplikacji**.

Kontenery będą widoczne pod portami:
- API + wszystkie endpointy actuatora (health, prometheus) – `8081` (`server.port=8081`; jeden wspólny port, bez osobnego portu na metryki)
- RabbitMQ – `15672` (GUI), `5672` (AMQP), `15692` (metryki Prometheus)
- MongoDB – `27017`
- Qdrant – `6334` (gRPC); port REST/Web UI jest efemeryczny (sprawdź przydzielony port hosta poleceniem `docker compose port qdrant 6333`)
- MinIO – `9000` (API), `9001` (konsola)
- Grafana – `3050` (mapowane na port `3000` wewnątrz kontenera)
- Prometheus – `9090`
- Zipkin – `9411`, Loki – `3100`

## 3. Sprawdzenie stanu pracy
```bash
docker compose ps
```

Aby zweryfikować, że aplikacja działa:
- `GET http://localhost:8081/actuator/health` → `UP`
- `GET http://localhost:8081/actuator/prometheus` → dane Prometheus
- `http://localhost:15672` – interfejs RabbitMQ w przeglądarce (login `admin`/`admin`, patrz `RABBITMQ_DEFAULT_USER`/`RABBITMQ_DEFAULT_PASS` w `docker-compose.yml`)
- Grafana pod `http://localhost:3050` – login `admin`/`example` (patrz `GF_SECURITY_ADMIN_PASSWORD` w `docker-compose.yml`)

## 4. Uruchamianie testów
```bash
./gradlew test
```
Uruchamia wszystkie testy jednostkowe **oraz** testy w pakiecie `.../integration/` — te ostatnie mockują warstwę infrastruktury (Mockito + `MockMvc`) i nie wymagają uruchomionych kontenerów. W repozytorium nie ma osobnego zadania Gradle do testów wymagających żywego Dockera.

## 5. Czyszczenie środowiska
```bash
docker compose --profile prod down -v
```
Flaga `--profile prod` gwarantuje zatrzymanie i usunięcie też kontenera `app`, jeśli był uruchomiony; `-v` usuwa wolumeny, dzięki czemu nie zostają żadne dane.

---
> **Tip** – plik `docker-compose.override.yml` nie jest jeszcze częścią repozytorium; możesz go samodzielnie utworzyć w trybie deweloperskim, aby nadpisać zmienne środowiskowe lub porty bez modyfikowania głównego `docker-compose.yml` (Compose scala oba pliki automatycznie, gdy override jest obecny).
