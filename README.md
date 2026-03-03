# Rekomenda API

REST + WebSocket API for personalized and collaborative film recommendations, powered by Gemini AI and TMDB.
This project was developed as part of the Software Analysis and Design Project course at UEPA, under the guidance of Professor Anderson Costa.
To learn more about the project's conceptualization and the methodologies applied, visit: https://gamma.app/docs/REKOMENDA-i5669qqb0jky9vi

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25 / Spring Boot 4 |
| Persistence | PostgreSQL 17 + Flyway |
| Cache / State | Redis 7 |
| AI | Google Vertex AI (Gemini 2.0 Flash) via Spring AI |
| Film data | TMDB REST API |
| Auth | JWT (stateless) + Spring Security 7 |
| Real-time | WebSockets (STOMP over SockJS) |
| Docs | Springdoc OpenAPI (Swagger UI) |

## Architecture

```
com.rekomenda.api
├── config/          # Spring beans: Security, JWT, Redis, WebSocket, OpenAPI, Scheduler
├── domain/
│   ├── auth/        # Register, login, password recovery (JWT issued here)
│   ├── user/        # Profile management; recommendation weight vector (JSONB)
│   ├── rating/      # Like/dislike/skip content; recalculates genre weights
│   ├── recommendation/ # Personal film feed built from genre weights × TMDB
│   ├── chat/        # Single-user AI chat → Gemini extracts intent → TMDB results
│   └── room/        # Collaborative rooms: REST lifecycle + STOMP real-time events
├── infrastructure/
│   ├── ai/          # GeminiService – keyword/genre extraction via Spring AI ChatClient
│   ├── tmdb/        # TmdbClient – movie search/detail via RestClient
│   └── mail/        # MailService – password-reset e-mails via JavaMailSender
└── shared/
    ├── exception/   # GlobalExceptionHandler, BusinessException, ErrorResponse
    └── security/    # JwtService, JwtAuthFilter, JwtChannelInterceptor, UserDetailsServiceImpl
```

### Key design decisions

- **Domain-centric packaging** — each domain owns its entity, repository, service, controller, and DTOs.
- **Stateless auth** — JWTs validated on every request; revoked tokens stored in Redis with TTL matching token expiry.
- **Recommendation weights** — each user holds a 'Map<genreId, score>' JSONB column updated incrementally on every rating; no batch job needed.
- **Rooms in Redis** — collaborative rooms are ephemeral (30-minute TTL); 'RoomCleanupScheduler' sweeps expired ones and broadcasts a 'ROOM_EXPIRED' STOMP event.
- **Host dropout handling** — 'RoomSessionEventListener' listens for WebSocket 'SessionDisconnectEvent' and closes the room if the host disconnects.

## API surface

| Prefix | Protocol | Description |
|---|---|---|
| '/api/auth/**' | REST | Register, login, logout, password recovery |
| '/api/users/me' | REST | Profile read/update |
| '/api/ratings' | REST | Rate content; view history |
| '/api/recommendations' | REST | Personalised film feed |
| '/api/chat/individual' | REST | Single-user AI recommendation chat |
| '/api/rooms/**' | REST | Create / join / inspect rooms |
| '/ws' | WebSocket (STOMP) | Room real-time events |
| '/swagger-ui.html' | HTTP | Interactive API docs |

### WebSocket message flow

```
Client → /app/room.{roomId}.join            → broadcasts RoomEvent(PARTICIPANT_JOINED)
Client → /app/room.{roomId}.submit-prompt   → triggers Gemini + TMDB, broadcasts FILMS_SUGGESTED
Client → /app/room.{roomId}.choose-film     → broadcasts FILM_CHOSEN
Client → /app/room.{roomId}.leave / kick / close / more-recommendations
Server → /topic/room.{roomId}               → all room events
```

## Running locally

### Prerequisites

- Docker + Docker Compose
- A '.env' file (copy from '.env.example' and fill in secrets)

```bash
cp .env.example .env
# edit .env with your TMDB key, Vertex AI project, SMTP credentials, JWT secret
docker compose up --build
```

The API will be available at 'http://localhost:8080'.
Swagger UI: 'http://localhost:8080/swagger-ui.html'

## Database migrations

Flyway runs automatically on startup. Migration scripts live in 'src/main/resources/db/migration/':

## CI / CD

Every push to 'main' triggers '.github/workflows/release.yml':

1. Builds a fat JAR ('mvnw package -DskipTests').
2. Computes a date-based version ('YYYYMMDD-N').
3. Creates a GitHub Release with the JAR as an artifact.
