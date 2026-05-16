# campus-multi-agent

`campus-multi-agent` is a campus-oriented multi-agent application for university learning and campus affairs scenarios. The project is built as a modular monolith in its first stage, while preserving clear package boundaries for future service extraction.

The system provides course question answering, course material summarization, study planning, campus affairs consultation, and todo management. AI capabilities are integrated through Alibaba Cloud Bailian/DashScope APIs via Spring AI Alibaba.

## Features

- **Authentication and authorization**: user registration, login, JWT issuance, and protected APIs.
- **Course management**: create and list personal courses.
- **Course material knowledge base**: upload text materials and index them into a vector database.
- **RAG retrieval**: retrieve user-scoped course material chunks from pgvector.
- **Multi-agent orchestration**: route user requests to course QA, study planning, material summary, or campus affairs agents.
- **Conversation history**: persist chat sessions and messages.
- **Todo management**: create, list, and update todo items.
- **Message queue extension point**: publish document indexing events to RocketMQ while keeping synchronous indexing for the first runnable version.
- **Web access**: serve the frontend through a project-scoped Nginx container.

## Architecture

```text
Browser
  -> Nginx container
    -> Static frontend
    -> Spring Boot API
      -> MySQL
      -> PostgreSQL + pgvector
      -> Redis
      -> RocketMQ
      -> Alibaba Bailian / DashScope
```

The project intentionally does not introduce Spring Cloud Gateway in the first version. Since the backend is currently a single deployable application, Nginx is enough for static file serving and reverse proxying. A gateway can be introduced later if the modules are split into independently deployed services.

## Technology Stack

- Java 21
- Maven
- Spring Boot 3.5.14
- Spring Security + OAuth2 Resource Server + JWT
- Spring AI 1.1.6
- Spring AI Alibaba DashScope 1.1.2.2
- MySQL 8.4
- PostgreSQL 16 + pgvector
- Redis 7.4
- RocketMQ 5.5.0
- Nginx 1.28

## Project Structure

```text
.
├── nginx/
│   ├── conf/                 # Project Nginx configuration
│   └── html/                 # Static frontend files served by Nginx
├── pgvector/
│   └── init/                 # pgvector initialization scripts
├── src/main/java/com/campus/agent/
│   ├── agent/                # Agent routing, orchestration, sessions, messages
│   ├── auth/                 # Login, registration, JWT issuance
│   ├── common/               # Shared API response and exception handling
│   ├── config/               # Security, CORS, pgvector datasource configuration
│   ├── course/               # Course domain
│   ├── document/             # Course materials and indexing events
│   ├── rag/                  # RAG indexing and retrieval
│   ├── todo/                 # Todo domain
│   └── user/                 # User domain
├── src/main/resources/
│   ├── application.yml       # Runtime configuration
│   ├── db/migration/         # Flyway migrations for MySQL
│   └── system-prompt/        # System prompts for each agent
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Agent Prompts

System prompts are stored as Markdown files under `src/main/resources/system-prompt`.
Each file corresponds to one agent role and is loaded by `AgentPromptFactory` at startup.
This keeps prompt engineering independent from Java business code.

## Configuration

Create a local `.env` file based on `.env.example`:

```bash
cp .env.example .env
```

Set the DashScope API key:

```bash
AI_DASHSCOPE_API_KEY=your_api_key
JWT_SECRET=campus-multi-agent-local-dev-secret-at-least-32-bytes
```

Sensitive local files such as `.env` are ignored by Git. Do not commit real API keys, database passwords, or JWT secrets.

If `AI_DASHSCOPE_API_KEY` is not configured, the application can still start. The agent layer will return a local fallback response so that authentication, course materials, RAG wiring, todos, and conversation persistence can be tested first.

## Run with Docker Compose

```bash
docker compose up --build
```

Then open:

```text
http://localhost:8088
```

Service ports:

| Service | Port |
| --- | --- |
| Web frontend via Nginx | `8088` |
| Spring Boot API | `8080` |
| MySQL | `3306` |
| pgvector | `5432` |
| Redis | `6379` |
| RocketMQ NameServer | `9876` |

## Build

```bash
mvn -DskipTests package
```

Download dependencies ahead of time:

```bash
mvn dependency:go-offline
```

## API Overview

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Login and receive a JWT |
| `GET` | `/api/courses` | List courses |
| `POST` | `/api/courses` | Create a course |
| `GET` | `/api/materials` | List course materials |
| `POST` | `/api/materials` | Upload and index course material text |
| `DELETE` | `/api/materials/{id}` | Delete material and related vector chunks |
| `POST` | `/api/agent/chat` | Send a message to the multi-agent orchestrator |
| `GET` | `/api/agent/sessions` | List chat sessions |
| `GET` | `/api/agent/sessions/{sessionId}/messages` | List messages in a session |
| `GET` | `/api/todos` | List todos |
| `POST` | `/api/todos` | Create a todo |
| `PATCH` | `/api/todos/{id}/status?status=DONE` | Update todo status |

## Roadmap

- Add PDF, PPT, DOCX parsing for course material uploads.
- Add production-grade RAG features, including configurable retrieval parameters, hybrid search, reranking, document delete/reindex workflows, and retrieval evaluation.
- Move document parsing and vector indexing fully behind RocketMQ consumers.
- Add SSE streaming responses for AI chat.
- Add Redis-backed conversation summary and hot question cache.
- Add school-system integrations only after a stable, authorized data access approach is confirmed.

## License

This project is currently for personal learning and development. Add a formal license before public distribution or external collaboration.
