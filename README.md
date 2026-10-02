English | [Polski](README.pl.md)

# PhotoFinder

An application for analyzing and searching photos using artificial intelligence. You send photos to a chosen AI provider (local or cloud-based), which generates descriptions. Those descriptions are stored in a vector database, enabling semantic photo search — e.g. "sunset over a lake" instead of a filename.

---

## Features

- Photo description via 13 different AI providers (local and cloud)
- Semantic search on photo descriptions (vector search)
- Asynchronous processing through message queues (RabbitMQ)
- Error handling via Dead Letter Queue with automatic auditing
- **ollama-worker** module — a lightweight app that runs on any machine in your LAN and processes photos using a local Ollama instance (CPU or GPU)
- EXIF data injected into the AI prompt (camera model, exposure time, ISO)
- Worker node identification (hostname + CPU) tracked in the audit log
- JWT-based authentication
- Monitoring dashboard (Prometheus + Grafana)
- REST API with OpenAPI/Swagger documentation

---

## Architecture

### Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        PhotoFinder App                       │
│                       (Spring Boot 3.5)                      │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │  photoparams │  │photosattribute│  │       user         │  │
│  │  (files)     │  │  (AI + desc)  │  │  (authentication)  │  │
│  └─────────────┘  └──────────────┘  └────────────────────┘  │
└──────────────────────────────┬───────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
    ┌─────▼─────┐      ┌───────▼──────┐     ┌──────▼──────┐
    │  MongoDB   │      │   RabbitMQ   │     │   Qdrant    │
    │ (metadata) │      │  (queues)    │     │  (vectors)  │
    └───────────┘      └──────┬───────┘     └─────────────┘
          │                   │
    ┌─────▼─────┐      ┌──────▼───────┐
    │   MinIO    │      │  ollama-     │  ← separate app
    │(temp files)│      │  worker      │    on any LAN machine
    └───────────┘      └──────────────┘
```

### Photo processing flow

```
REST API → MongoDB (PENDING) → MinIO (tmp) → RabbitMQ (ai.<provider>.queue)
                                                        │
                        ┌───────────────────────────────┤
                        │ built-in PhotosConsumer        │ external OllamaWorker
                        │ (all providers)                │ (ollama-worker on another PC)
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
                                              Semantic search ← REST API
```

### Error handling (DLQ)

```
ai.<provider>.queue → 3 retries → DLQ
                                   │
                         DeadLetterConsumer
                                   │
                    MongoDB (ERROR) + PhotoAuditedEvent
```

### Code structure (hexagonal architecture)

```
photofinder/
├── src/main/java/eu/mm/software/photofinder/
│   ├── common/                    # configuration, security, metrics
│   │   ├── config/                # RabbitMQ, AI, Async
│   │   ├── security/              # JWT, Spring Security
│   │   └── metrics/               # Prometheus metrics
│   │
│   ├── photosattribute/           # core module — AI analysis and descriptions
│   │   ├── domain/                # entities, repository interfaces, events
│   │   ├── application/           # business logic (command/query)
│   │   ├── infrastructure/
│   │   │   ├── ai/                # implementations for 13 AI providers
│   │   │   ├── rabbit/            # RabbitMQ consumers and producers
│   │   │   │   ├── PhotosConsumer           # built-in AI processing
│   │   │   │   ├── PhotoPersistenceConsumer # saves to MongoDB + audit event
│   │   │   │   ├── EmbeddedConsumer         # generates embeddings (Qdrant)
│   │   │   │   ├── AuditConsumer            # writes audit records
│   │   │   │   └── DeadLetterConsumer       # handles DLQ failures
│   │   │   ├── repository/        # MongoDB
│   │   │   ├── vectordb/          # Qdrant
│   │   │   └── starage/           # MinIO
│   │   └── interfaces/rest/       # REST controllers
│   │
│   ├── photoparams/               # browsing files on disk
│   └── user/                      # user management
│
└── ollama-worker/                 # separate app — CPU/GPU worker node
    └── src/main/java/eu/mm/software/photofinder/worker/
        ├── OllamaWorkerApplication    # Spring Boot (non-web)
        ├── OllamaWorkerConsumer       # listens on ai.ollama.queue
        ├── WorkerNodeInfo             # auto-detects hostname + CPU
        ├── config/                    # RabbitMQ, MinIO
        ├── event/                     # PhotoJobMessage, PhotoProcessedEvent
        └── storage/                   # WorkerMinioStorage
```

### Supported AI providers

| Provider      | Type   | Paid  |
|---------------|--------|-------|
| Ollama        | local  | no    |
| Groq          | cloud  | no    |
| Cerebras      | cloud  | no    |
| CloudFlare    | cloud  | no    |
| Gemini        | cloud  | no    |
| OpenRouter    | cloud  | no    |
| Cohere        | cloud  | no    |
| Mistral       | cloud  | yes*  |
| NVIDIA NIM    | cloud  | yes*  |
| Together AI   | cloud  | yes   |
| OpenAI        | cloud  | yes   |
| Anthropic     | cloud  | yes   |
| Hyperbolic    | cloud  | yes   |

\* free tier available

---

## Requirements

- Java 21
- Docker and Docker Compose
- NVIDIA GPU with CUDA (for Ollama with GPU acceleration) — optional
- API keys for chosen cloud providers

---

## Getting started

### 1. Clone the repository

```bash
git clone <repository-url>
cd photofinder
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
# Required
MONGO_PASSWORD=your_mongo_password
JWT_SECRET_KEY=generate_with_command_below

# AI providers (fill in only the ones you want to use)
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

# MinIO (optional — defaults work locally)
MINIO_USER=minioadmin
MINIO_PASSWORD=miniopassword
```

Generate a JWT secret key:
```bash
openssl rand -base64 64
```

### 3. Start infrastructure (development mode)

Spring Boot automatically starts Docker Compose on launch. Just run:

```bash
./gradlew bootRun
```

On first run Docker will pull all required images: MongoDB, RabbitMQ, Qdrant, MinIO, Ollama, Prometheus, Grafana, and more.

### 4. Full Docker deployment (production mode)

Build the application image and start all services:

```bash
./gradlew bootWar
docker compose --profile prod up -d
```

### 5. Pull Ollama models

Once the Ollama container is running, pull the required models:

```bash
# Vision model for photo description
docker exec ollama-gpu ollama pull llava:13b

# Embedding model for vector search
docker exec ollama-gpu ollama pull qllama/bge-large-en-v1.5
```

---

## ollama-worker module

`ollama-worker` is a lightweight, standalone Spring Boot application (no HTTP server) that you can run on any machine in your local network. It picks up jobs from a RabbitMQ queue, processes photos using a local Ollama instance, and sends results back to the main application.

Useful when:
- you have multiple machines with different GPUs/CPUs and want to distribute the workload
- the main server has no GPU, but other machines on the network do

### Configuration (`ollama-worker/src/main/resources/application.properties`)

```properties
# Node label (blank = auto-detect from hostname + /proc/cpuinfo)
worker.node.name=Living-Room-PC

# Ollama model (must be pulled on this machine)
ollama.model.name=llava:13b

# Address of the main photofinder server
spring.rabbitmq.host=192.168.0.35
minio.endpoint=http://192.168.0.35:9000

# Local Ollama instance
spring.ai.ollama.base-url=http://localhost:11434
```

### Running

```bash
cd ollama-worker
../gradlew bootRun
```

The worker auto-detects its hostname and CPU model from `/proc/cpuinfo`. Both are recorded in the audit entry for every processed photo.

---

## Service URLs

| Service           | URL                                    | Notes                       |
|-------------------|----------------------------------------|-----------------------------|
| REST API          | http://localhost:8081                  | main API                    |
| Swagger UI        | http://localhost:8081/swagger-ui.html  | API documentation           |
| RabbitMQ Panel    | http://localhost:15672                 | login: admin / admin        |
| MongoDB           | localhost:27017                        |                             |
| Qdrant UI         | http://localhost:6333/dashboard        | vector database             |
| MinIO Console     | http://localhost:9001                  | file storage                |
| Ollama            | http://localhost:11434                 |                             |
| Open WebUI        | http://localhost:3500                  | chat with local models      |
| Prometheus        | http://localhost:9090                  | metrics                     |
| Grafana           | http://localhost:3050                  | login: admin / example      |

---

## API usage examples

### Register and log in

```bash
# Register
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","firstName":"John","lastName":"Doe"}'

# Log in — returns a JWT token
curl -X POST http://localhost:8081/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

### List photos on disk

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/file?path=/media/nas/foto&extensions=JPG,PNG"
```

### Describe a photo using AI

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/photos/describePhoto?filepath=/media/nas/foto/IMG_001.jpg&provider=OLLAMA"
```

### Semantic search

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/photos/search?prompt=sunset+over+the+sea&limit=10"
```

### View audit log

```bash
# Audit entries for the logged-in user
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/audit"

# Audit stats (processing time, tokens, worker node)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8081/rest/api/v1/audit/stats"
```

---

## Monitoring

The application exports metrics to Prometheus at `/actuator/prometheus`. Data is collected from:

- the application (JVM, HTTP, custom business metrics)
- RabbitMQ (queues, consumers)
- MongoDB
- MinIO
- NVIDIA GPU (VRAM, temperature, load)
- the host system (CPU, RAM, disk)
- Docker containers

Ready-to-use Grafana dashboards can be imported using these IDs from grafana.com:
- **4701** — JVM (Micrometer)
- **10991** — RabbitMQ
- **7362** — MongoDB

---

## Tech stack

| Category         | Technology                               |
|------------------|------------------------------------------|
| Backend          | Spring Boot 3.5, Java 21                 |
| Build            | Gradle (multi-project)                   |
| Database         | MongoDB (metadata), Qdrant (vectors)     |
| Messaging        | RabbitMQ                                 |
| Storage          | MinIO (S3-compatible)                    |
| AI Framework     | Spring AI 1.1                            |
| Local LLM        | Ollama                                   |
| Security         | Spring Security, JWT                     |
| Monitoring       | Prometheus, Grafana, Micrometer          |
| API Docs         | SpringDoc OpenAPI (Swagger)              |
| Containerization | Docker, Docker Compose                   |

## License

This project is licensed under the [GNU AGPL v3.0](LICENSE). Derivative works, including those offered as a network service, must be released under the same license with their source code.
