# Observability: logi (Loki) + traces (Zipkin) w Grafanie

Dwa brakujące filary obok Prometheusa (metryki):

```
metryki:  app /actuator/prometheus  ── Prometheus ─┐
logi:     kontenery ── Promtail ── Loki ───────────┼── Grafana
traces:   app (Micrometer) ── Zipkin ──────────────┘   (+ natywne UI Zipkina :9411)
```

Wszystko startuje z rootowego `docker-compose.yml`.

---

## LOGI — Loki + Promtail
- **loki** (`:3100`) — składuje/indeksuje logi (dane w `~/docker/photos_finder/data/loki`).
- **promtail** — czyta stdout/stderr **wszystkich** kontenerów przez socket Dockera → Loki.
- Datasource **Loki** dodany do Grafany automatycznie.

## TRACES — Zipkin
- **zipkin** (`:9411`) — zbiera i pokazuje ślady requestów.
- Aplikacja instrumentowana przez **Micrometer Tracing** (Brave) + eksport do Zipkina
  (zależności w `build.gradle`).
- Datasource **Zipkin** dodany do Grafany automatycznie.

### ⚠️ Wymagana konfiguracja w `application.properties`
(Nie mogłem edytować tego pliku — dodaj ręcznie.)
```properties
# Traces → Zipkin
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
```
- `sampling.probability=1.0` = śledź **każdy** request (do testów; na produkcji zwykręć np. 0.1).
- **W profilu `docker`** (app w kontenerze) endpoint musi wskazywać kontener, nie localhost —
  w `application-docker.properties` ustaw:
  ```properties
  management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
  ```
- Bonus: gdy tracing jest na classpath, Spring Boot **automatycznie** dokleja `traceId`/`spanId`
  do linii logów → korelacja Loki ↔ Zipkin (patrz niżej).

### ollama-worker — traces end-to-end (app → RabbitMQ → worker)
Worker jest instrumentowany, a `RabbitTemplate`/listenery po obu stronach mają włączone
**Micrometer Observation** (w kodzie: `ConfigRabbit` i `WorkerRabbitConfig`), więc kontekst
trace propaguje się przez nagłówki wiadomości. W `ollama-worker/.../application.properties` dodaj:
```properties
management.tracing.sampling.probability=1.0
# adres Zipkina widziany z węzła workera (GPU/Kaggle) — NIE localhost, tylko host głównej apki:
management.zipkin.tracing.endpoint=http://192.168.0.35:9411/api/v2/spans
```
Efekt: jeden ślad obejmuje request HTTP w apce → publikację na kolejkę → przetwarzanie w workerze.

---

## Jak sprawdzić, że działa

### 0. Start
```bash
docker compose up -d loki promtail zipkin grafana prometheus
# uruchom aplikację (z profilem dev lokalnie albo docker), zrób kilka requestów, np.:
curl -i http://localhost:8081/rest/api/v1/authenticate -H 'Content-Type: application/json' \
  -d '{"userEmail":"x@y.pl","password":"zle"}'      # wygeneruje trace + logi
```

### 1. Loki (logi)
- **Grafana** http://localhost:3050 → **Explore** → źródło **Loki**:
  ```logql
  {container="photoFinder-app"}                 # logi apki
  {container="photoFinder-app"} |= "ERROR"      # tylko błędy
  ```
- **Szybki test API** (bez Grafany) — czy Loki ma etykiety:
  ```bash
  curl -s "http://localhost:3100/loki/api/v1/labels"        # powinno zwrócić m.in. "container"
  curl -s "http://localhost:3100/loki/api/v1/label/container/values"
  ```
- Zdrowie Loki: `curl -s http://localhost:3100/ready` → `ready`.

### 2. Zipkin (traces)
- **UI** http://localhost:9411 → **Run Query** → zobaczysz ślady (np. `POST /rest/api/v1/authenticate`).
  Kliknij ślad → oś czasu spanów (HTTP, ewentualnie Mongo/Rabbit).
- **Szybki test API**:
  ```bash
  curl -s "http://localhost:9411/api/v2/services"           # lista usług, np. ["photofinder"]
  ```
- Jeśli pusto: zrób najpierw request do aplikacji i sprawdź, że `sampling.probability=1.0`
  oraz że endpoint pasuje do środowiska (localhost vs zipkin).

### 3. Korelacja logi ↔ traces
1. W logu aplikacji znajdź `traceId` (Spring dokleja go automatycznie: `[photofinder,<traceId>,<spanId>]`).
2. Wklej `traceId` w Zipkinie (pole „Search by trace ID") → zobaczysz pełną ścieżkę requestu.
3. W drugą stronę: z Zipkina bierzesz `traceId`, w Loki: `{container="photoFinder-app"} |= "<traceId>"`.

---

## Uwagi
- Wersje przypięte: loki/promtail `3.4.2`, zipkin `3`.
- Zipkin domyślnie trzyma ślady **w pamięci** (znikają po restarcie) — do lokalnego debugowania OK.
- Cross-service tracing app → RabbitMQ → `ollama-worker` **działa** (observation włączone po obu
  stronach). W Zipkinie zobaczysz spany obu usług w jednym śladzie.
