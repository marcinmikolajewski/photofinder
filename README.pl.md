[English](README.md) | Polski

# PhotoFinder

Aplikacja do analizy i wyszukiwania zdjęć przy użyciu sztucznej inteligencji. Wysyłasz zdjęcia do wybranego dostawcy AI (lokalnego lub chmurowego), który generuje opisy. Opisy trafiają do wektorowej bazy danych, dzięki czemu możesz wyszukiwać zdjęcia po treści naturalnym językiem — np. „zachód słońca nad jeziorem" zamiast nazwy pliku.

---

## Funkcje

- Opis zdjęć przez 13 różnych dostawców AI (lokalnych i chmurowych)
- Wyszukiwanie semantyczne po opisie (vector search)
- Asynchroniczne przetwarzanie przez kolejki wiadomości (RabbitMQ)
- Obsługa błędów przez Dead Letter Queue z automatycznym audytem
- Moduł **ollama-worker** — lekka aplikacja uruchamiana na dowolnym komputerze w sieci, która przetwarza zdjęcia lokalnym Ollama (CPU lub GPU)
- Dane EXIF dołączane do promptu AI (model kamery, czas naświetlania, ISO)
- Identyfikacja węzła roboczego (hostname + CPU) w audycie
- JWT-based uwierzytelnianie
- Panel monitorowania (Prometheus + Grafana)
- REST API z dokumentacją OpenAPI/Swagger

---

## Architektura

### Ogólny schemat

```
┌─────────────────────────────────────────────────────────────┐
│                        PhotoFinder App                      │
│                       (Spring Boot 3.5)                     │
│                                                             │
│  ┌─────────────┐  ┌───────────────┐ ┌────────────────────┐  │
│  │  photoparams│  │photosattribute│ │       user         │  │
│  │  (pliki)    │  │  (AI + opisy) │ │ (autentykacja)     │  │
│  └─────────────┘  └───────────────┘ └────────────────────┘  │
└──────────────────────────────┬──────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
    ┌─────▼─────┐      ┌───────▼──────┐     ┌──────▼──────┐
    │  MongoDB  │      │   RabbitMQ   │     │   Qdrant    │
    │ (metadane)│      │  (kolejki)   │     │  (wektory)  │
    └───────────┘      └──────┬───────┘     └─────────────┘
          │                   │
    ┌─────▼─────┐      ┌──────▼───────┐
    │   MinIO   │      │  ollama-     │  ← osobna aplikacja
    │(tmp pliki)│      │  worker      │    w sieci lokalnej
    └───────────┘      └──────────────┘
```

### Przepływ przetwarzania zdjęcia

```
REST API → MongoDB (PENDING) → MinIO (tmp) → RabbitMQ (ai.<provider>.queue)
                                                        │
                        ┌───────────────────────────────┤
                        │ wbudowany PhotosConsumer       │ zewnętrzny OllamaWorker
                        │ (wszystkie providery)          │ (ollama-worker na innym PC)
                        └───────────────┬───────────────┘
                                        │
                              PhotoProcessedEvent
                                        │
                              app.photo.save.queue
                                        │
                          PhotoPersistenceConsumer
                                        │
                          MongoDB (DESCRIBED / ERROR)
                                        │
                   ┌────────────────────┴────────────────────┐
                   │ PhotoAuditedEvent                        │ PhotoDescribedEvent
                   │                                          │
              AuditConsumer                          ai.embedded.queue
              (AuditPhotos)                                   │
                                                    EmbeddedConsumer
                                                    Ollama (embedding)
                                                    Qdrant (vector store)
                                                              │
                                              Wyszukiwanie ← REST API
```

### Obsługa błędów (DLQ)

```
ai.<provider>.queue → 3 retry → DLQ
                                 │
                       DeadLetterConsumer
                                 │
                    MongoDB (ERROR) + PhotoAuditedEvent
```

### Struktura kodu (architektura heksagonalna)

```
photofinder/
├── src/main/java/eu/mm/software/photofinder/
│   ├── common/                    # konfiguracja, security, metryki
│   │   ├── config/                # RabbitMQ, AI, Async
│   │   ├── security/              # JWT, Spring Security
│   │   └── metrics/               # metryki Prometheus
│   │
│   ├── photosattribute/           # główny moduł — analiza AI i opisy
│   │   ├── domain/                # encje, repozytoria (interfejsy), eventy
│   │   ├── application/           # logika biznesowa (command/query)
│   │   ├── infrastructure/
│   │   │   ├── ai/                # implementacje 13 dostawców AI
│   │   │   ├── rabbit/            # konsumery i producenci RabbitMQ
│   │   │   │   ├── PhotosConsumer         # przetwarzanie przez AI (wbudowane)
│   │   │   │   ├── PhotoPersistenceConsumer # zapis do MongoDB + audit event
│   │   │   │   ├── EmbeddedConsumer       # generowanie embeddingów (Qdrant)
│   │   │   │   ├── AuditConsumer          # zapis audytu
│   │   │   │   └── DeadLetterConsumer     # obsługa DLQ
│   │   │   ├── repository/        # MongoDB
│   │   │   ├── vectordb/          # Qdrant
│   │   │   └── starage/           # MinIO
│   │   └── interfaces/rest/       # kontrolery REST
│   │
│   ├── photoparams/               # przeglądanie plików na dysku
│   └── user/                      # zarządzanie użytkownikami
│
└── ollama-worker/                 # oddzielna aplikacja — węzeł CPU/GPU
    └── src/main/java/eu/mm/software/photofinder/worker/
        ├── OllamaWorkerApplication    # Spring Boot (non-web)
        ├── OllamaWorkerConsumer       # nasłuchuje ai.ollama.queue
        ├── WorkerNodeInfo             # auto-detect hostname + CPU
        ├── config/                    # RabbitMQ, MinIO
        ├── event/                     # PhotoJobMessage, PhotoProcessedEvent
        └── storage/                   # WorkerMinioStorage
```

### Obsługiwani dostawcy AI

| Dostawca      | Typ      | Płatny |
|---------------|----------|--------|
| Ollama        | lokalny  | nie    |
| Groq          | chmura   | nie    |
| Cerebras      | chmura   | nie    |
| CloudFlare    | chmura   | nie    |
| Gemini        | chmura   | nie    |
| OpenRouter    | chmura   | nie    |
| Cohere        | chmura   | nie    |
| Mistral       | chmura   | tak*   |
| NVIDIA NIM    | chmura   | tak*   |
| Together AI   | chmura   | tak    |
| OpenAI        | chmura   | tak    |
| Anthropic     | chmura   | tak    |
| Hyperbolic    | chmura   | tak    |

\* darmowy plan dostępny

---

## Wymagania

- Java 21
- Docker i Docker Compose
- Karta graficzna NVIDIA z CUDA (dla Ollama z GPU) — opcjonalne
- Klucze API dla wybranych dostawców chmurowych

---

## Uruchomienie

### 1. Klonowanie repozytorium

```bash
git clone <url-repozytorium>
cd photofinder
```

### 2. Konfiguracja zmiennych środowiskowych

Utwórz plik `.env` w katalogu głównym projektu:

```env
# Wymagane
MONGO_PASSWORD=twoje_haslo_mongo
JWT_SECRET_KEY=wygeneruj_kluczem_ponizej

# Dostawcy AI (uzupełnij tylko te, których chcesz używać)
MISTRAL_API_KEY=
NVIDIA_API_KEY=
OPENAI_API_KEY=
GROQ_API_KEY=
TOGETHER_API_KEY=
HYPERBOLIC_API_KEY=
GOOGLE_API_KEY=
CEREBRAS_API_KEY=
ANTHROPIC_API_KEY=
COHERE_API_KEY=
OPENROUTER_API_KEY=
CLOUDFLARE_API_KEY=
CLOUD_FLARE_USER_ID=

# MinIO (opcjonalne — domyślne wartości działają lokalnie)
MINIO_USER=minioadmin
MINIO_PASSWORD=miniopassword
```

Generowanie klucza JWT:
```bash
openssl rand -base64 64
```

### 3. Uruchomienie infrastruktury (tryb developerski)

Spring Boot automatycznie uruchamia Docker Compose przy starcie. Wystarczy uruchomić aplikację:

```bash
./gradlew bootRun
```

Przy pierwszym uruchomieniu Docker pobierze obrazy: MongoDB, RabbitMQ, Qdrant, MinIO, Ollama, Prometheus, Grafana i inne.

### 4. Uruchomienie całości w Dockerze (tryb produkcyjny)

Zbuduj obraz aplikacji i uruchom wszystkie usługi:

```bash
./gradlew bootWar
docker compose --profile prod up -d
```

### 5. Pobranie modeli do Ollamy

Po starcie kontenera Ollama pobierz modele:

```bash
# Model do opisu zdjęć (vision)
docker exec ollama-gpu ollama pull llava:13b

# Model do embeddingów (wyszukiwanie wektorowe)
docker exec ollama-gpu ollama pull qllama/bge-large-en-v1.5
```

---

## Moduł ollama-worker

`ollama-worker` to lekka, samodzielna aplikacja Spring Boot (bez serwera HTTP), którą możesz uruchomić na dowolnym komputerze w sieci lokalnej. Pobiera zadania z kolejki RabbitMQ, przetwarza zdjęcia lokalnym Ollama i odsyła wyniki do głównej aplikacji.

Przydatna gdy:
- masz kilka komputerów z różnymi GPU/CPU i chcesz rozłożyć przetwarzanie
- główny serwer nie ma GPU, ale inne maszyny w sieci już tak

### Konfiguracja (`ollama-worker/src/main/resources/application.properties`)

```properties
# Identyfikacja węzła (pusta = auto-detect z hostname + /proc/cpuinfo)
worker.node.name=PC-Salon

# Model Ollama (musi być pobrany na tym komputerze)
ollama.model.name=llava:13b

# Adres głównego komputera z photofinder
spring.rabbitmq.host=192.168.0.35
minio.endpoint=http://192.168.0.35:9000

# Lokalny Ollama
spring.ai.ollama.base-url=http://localhost:11434
```

### Uruchomienie

```bash
cd ollama-worker
../gradlew bootRun
```

Worker automatycznie wykrywa hostname i model CPU z `/proc/cpuinfo`. Obie informacje zapisywane są w audycie każdego przetworzonego zdjęcia.

---

## Adresy serwisów po uruchomieniu

| Serwis            | Adres                                  | Opis                        |
|-------------------|----------------------------------------|-----------------------------|
| Aplikacja REST    | http://localhost:8081                  | główne API                  |
| Swagger UI        | http://localhost:8081/swagger-ui.html  | dokumentacja API            |
| RabbitMQ Panel    | http://localhost:15672                 | login: admin / admin        |
| MongoDB           | localhost:27017                        |                             |
| Qdrant UI         | http://localhost:6333/dashboard        | baza wektorów               |
| MinIO Console     | http://localhost:9001                  | przechowywanie plików       |
| Ollama            | http://localhost:11434                 |                             |
| Open WebUI        | http://localhost:3500                  | chat z lokalnymi modelami   |
| Prometheus        | http://localhost:9090                  | metryki                     |
| Grafana           | http://localhost:3050                  | login: admin / example      |

---

## Przykładowe użycie API

### Rejestracja i logowanie

```bash
# Rejestracja
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"haslo123","firstName":"Jan","lastName":"Kowalski"}'

# Logowanie — zwraca token JWT
curl -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"haslo123"}'
```

### Wylistowanie zdjęć na dysku

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/file?path=/media/nas/foto&extensions=JPG,PNG"
```

### Opisanie zdjęcia przez AI

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/photos/describePhoto?filepath=/media/nas/foto/IMG_001.jpg&provider=OLLAMA"
```

### Wyszukiwanie semantyczne

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/photos/search?prompt=zachód słońca nad morzem&limit=10"
```

### Podgląd audytu

```bash
# Lista audytów dla zalogowanego użytkownika
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/audit"

# Statystyki audytu (czas przetwarzania, tokeny, węzeł)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/audit/stats"
```

---

## Monitoring

Aplikacja eksportuje metryki do Prometheusa (`/actuator/prometheus`). Zbierane są dane z:

- aplikacji (JVM, HTTP, własne metryki biznesowe)
- RabbitMQ (kolejki, konsumery)
- MongoDB
- MinIO
- GPU NVIDIA (VRAM, temperatura, obciążenie)
- systemu (CPU, RAM, dysk)
- kontenerów Docker

Gotowe dashboardy można zaimportować do Grafany z grafana.com korzystając z ID:
- **4701** — JVM (Micrometer)
- **10991** — RabbitMQ
- **7362** — MongoDB

---

## Stos technologiczny

| Kategoria        | Technologia                              |
|------------------|------------------------------------------|
| Backend          | Spring Boot 3.5, Java 21                 |
| Build            | Gradle (multi-project)                   |
| Baza danych      | MongoDB (metadane), Qdrant (wektory)     |
| Kolejkowanie     | RabbitMQ                                 |
| Storage          | MinIO (S3-compatible)                    |
| AI Framework     | Spring AI 1.1                            |
| Lokalne LLM      | Ollama                                   |
| Bezpieczeństwo   | Spring Security, JWT                     |
| Monitoring       | Prometheus, Grafana, Micrometer          |
| Dokumentacja API | SpringDoc OpenAPI (Swagger)              |
| Konteneryzacja   | Docker, Docker Compose                   |

## Licencja

Projekt jest udostępniany na licencji [GNU AGPL v3.0](LICENSE). Pochodne wersje, także udostępniane jako usługa sieciowa, muszą być publikowane na tej samej licencji wraz z kodem źródłowym.
