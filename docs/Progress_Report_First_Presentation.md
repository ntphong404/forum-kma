## Báo cáo tiến độ dự án - Buổi 1

Tệp này tóm tắt toàn bộ tiến độ, kiến trúc, công nghệ, các chức năng đã hoàn thành, các việc còn làm, rủi ro và checklist demo cho buổi báo cáo đầu tiên. Viết bằng tiếng Việt để trình bày trực tiếp.

## 1. Tổng quan dự án

Mục tiêu: Xây dựng một cổng forum (microservices) bao gồm các dịch vụ chính: API Gateway, Auth Service, Post Service, Comment Service, Notification Service, Eureka (service discovery), Common library, và các dịch vụ mở rộng trong tương lai (File service, Chat service, Search service, Chatbot service - Rasa).

Kiến trúc: microservices (mỗi thành phần là một service Spring Boot riêng biệt) sử dụng:
- Service discovery: Eureka
- API Gateway: Spring Cloud Gateway
- Messaging: Kafka (reactive consumer/producer via reactor-kafka)
- Persistence: R2DBC với PostgreSQL (reactive)
- Cache/session: Redis (reactive client)
- Security: JWT (Nimbus JOSE utilities in `common` module)
- Reactive stack: Spring WebFlux, Project Reactor

## 2. Ngôn ngữ & công nghệ chính (tóm tắt)

- Java 21, Spring Boot 3.x (WebFlux)
- Spring Cloud Gateway (API Gateway)
- Reactor Kafka (reactive Kafka client)
- R2DBC (reactive DB access to PostgreSQL)
- Reactive Redis (session storage)
- MapStruct (data mapping in post-service)
- WebClient (non-blocking HTTP client) — dùng cho Brevo email API
- Gradle (per-module wrappers) để build

Ưu điểm so với các giải pháp khác (tập trung vào API Gateway):

- Spring Cloud Gateway (SCG) so với Kong / Traefik / NGINX:
  - Tích hợp sâu với Spring ecosystem: dễ dàng dùng `JwtUtil`, security, config properties trong `common` module; nếu backend đa số là Spring thì SCG cho trải nghiệm phát triển hài hoà.
  - Reactive by default: SCG hoạt động tốt với stack WebFlux, giảm blocking thread khi xử lý yêu cầu nhiều I/O.
  - Định tuyến lập trình được (Java config) và filter dễ mở rộng (ví dụ add header, validate JWT, call internal services).
  - Tuy nhiên: Kong/Traefik/NGINX có hiệu năng cực kỳ tốt ở layer L4/L7, có nhiều plugin và triển khai dễ dàng trong môi trường non-Java; nên nếu cần throughput cực lớn, cân nhắc dịch vụ Gateway chuyên dụng.

## 3. Những gì nhóm đã hoàn thành (chi tiết theo service)

Lưu ý: đường dẫn file dưới repository tương ứng với dự án multi-module.

- common
  - Thư viện dùng chung: `JwtUtil`, `JwtProperties`, `JwtClaims` để tạo/validate JWT, các exception/response utils.

- api-gateway
  - Thêm filter để kiểm tra JWT (dùng `JwtUtil` từ `common`) và gắn context user cho downstream services.
  - Định tuyến đến các service nội bộ (e.g., /api/auth, /api/posts, /api/comments) — reusable patterns.

- auth-service
  - Đã implement login/register/refresh flows.
  - Session management: dùng Redis Hash để lưu các session (field=sessionId -> value=DeviceInfo). Implemented methods in `SessionService`:
    - `addSession(userId, sessionId, deviceInfo)`
    - `isSessionActive(userId, sessionId)`
    - `revokeSingleSession(userId, sessionId)`
    - `revokeAllSessions(userId)`
    - `getAllSessions(userId)`
  - Added `SessionController` endpoints to allow users to:
    - GET `/auth/sessions` — liệt kê thiết bị/phiên
    - DELETE `/auth/sessions/{sessionId}` — thu hồi 1 phiên
    - DELETE `/auth/sessions` — thu hồi tất cả phiên
  - Security: JWT authentication web filter populates Reactive SecurityContext với `User` principal; controller dùng `ReactiveSecurityContextHolder` để lấy user hiện tại.

- post-service
  - CRUD cơ bản cho posts (MapStruct used for mapping DTO <-> Entity).
  - CRUD cơ bản cho comments.

- notification-service
  - Reactive Kafka consumer nhận sự kiện (ví dụ post created) và gửi email thông báo.
  - Tạo interface `EmailSender` và `BrevoEmailSender` impl dùng `WebClient` non-blocking để gọi Brevo SMTP API (JSON + `api-key` header).
  - `PostEventConsumer` được chỉnh sửa thành pipeline reactive: parse event -> build `EmailRequest` -> call `emailSender.send(...)` -> acknowledge Kafka offset sau khi gửi thành công.

- eureka-server
  - Chạy service discovery để tự động đăng ký các service.

- postman
  - Tập hợp collection `forum-kma.postman_collection.json` đã cập nhật, chứa request cho auth, posts, comments và Session endpoints mới.

## 4. Cách hoạt động chính (flow highlights)

- Đăng nhập/Đăng ký:
  - Client gọi gateway `/api/auth/login` -> route tới `auth-service`.
  - `AuthService` xác thực -> tạo sessionId, lưu session vào Redis, tạo access token (short-lived) và refresh token (long-lived, chứa sid = sessionId).

- Bảo mật và Authorization:
  - Gateway filter validate JWT và đặt SecurityContext. Các service downstream sử dụng `ReactiveSecurityContextHolder` hoặc header `X-User-Id`/`X-Session-Id` nếu cần.

- Notification:
  - `post-service` tạo post -> publish event to Kafka.
  - `notification-service` consumes event reactively, gửi email bằng `BrevoEmailSender` (non-blocking). Offset Kafka được ack sau khi mail gửi thành công.

## 5. Demo checklist (để trình bày trong buổi báo cáo)

Chuẩn bị môi trường:
- Đảm bảo Docker (Kafka, Zookeeper, PostgreSQL, Redis) đang chạy theo file docker-compose (nếu có).
- Các biến môi trường quan trọng: `BREVO_API_KEY` (khuyến nghị không commit key vào repo), DB URL, Redis URL.

Thao tác demo:
1. Chạy Eureka server
2. Chạy `auth-service`, `post-service`, `notification-service`, `api-gateway` (dùng Gradle wrapper):

```powershell
.\gradlew.bat :eureka-server:bootRun
.\gradlew.bat :auth-service:bootRun
.\gradlew.bat :post-service:bootRun
.\gradlew.bat :notification-service:bootRun
.\gradlew.bat :api-gateway:bootRun
```

3. Dùng Postman collection `forum-kma.postman_collection.json` (import file) để:
  - Register và login -> lấy `accessToken`/`refreshToken`.
  - Tạo post (POST `/api/posts`) -> kiểm tra Kafka topic và xem notification-service gửi email.
  - Kiểm tra /auth/sessions (GET `/api/auth/sessions`) -> show các device/session.
  - Revoke 1 session (DELETE `/api/auth/sessions/{sessionId}`) -> verify session removed in Redis.

4. Logs: show how `notification-service` logs email send and Kafka ack sequence.

## 6. Công việc còn làm (ưu tiên & timeline đề xuất)

Ưu tiên cao (trong sprint tiếp theo):
1. Fix build & CI
   - Chạy build cho từng module (`auth-service`, `notification-service`, `post-service`) và sửa mọi lỗi compile. (Hiện có báo cáo lỗi cú pháp cũ trong `AuthService.java` đã được lưu ý; nếu cần tôi sẽ sửa trực tiếp.)
2. Secrets management
   - Di chuyển `email.brevo.apiKey` ra biến môi trường `BREVO_API_KEY` hoặc secrets manager; cập nhật `application.yml` để không commit key.
3. Tests cơ bản
   - Unit tests cho `BrevoEmailSender` (mock WebClient), `SessionController` (mock SessionService) và một số integration tests cho flows chính.
4. Retry & DLQ cho notification
   - Hiện consumer ack sau khi gửi mail. Cần bổ sung retry/backoff & DLQ để xử lý transient failures an toàn.

Tầm trung (next sprints):
5. File service
   - Lưu file người dùng (profile pics, attachments), tích hợp object storage (S3/minio), endpoint upload/download, virus scanning optional.
6. Chat service
   - Real-time chat (WebSocket), lưu message, typing indicators, online presence. Sử dụng Redis pub/sub hoặc Kafka.
7. Search service
   - Full-text search cho posts/comments (Elasticsearch/Opensearch). Indexing service, sync khi tạo/sửa bài.
8. Chatbot service (Rasa)
   - Tạo chatbot hỗ trợ người dùng: FAQ, hướng dẫn, mod tools. Tích hợp Rasa với Chat service qua webhook.

Tác vụ bảo trì & nâng cao:
- Monitoring & Observability: Micrometer + Prometheus + Grafana, distributed tracing (Zipkin/Jaeger).
- Logging & centralization: ELK/EFK stack.
- Autoscaling & containerization: Docker images, k8s manifests, readiness/liveness checks.

## 7. Kiến trúc dữ liệu & API (tóm tắt endpoints quan trọng)

- Auth:
  - POST `/api/auth/register` -> register
  - POST `/api/auth/login` -> login (returns accessToken & refreshToken)
  - POST `/api/auth/refresh` -> refresh tokens
  - GET `/api/auth/sessions` -> list sessions
  - DELETE `/api/auth/sessions/{sessionId}` -> revoke session
  - DELETE `/api/auth/sessions` -> revoke all sessions

- Post:
  - CRUD `/api/posts` (create, get, update, delete)

- Comment:
  - CRUD `/api/comments`

- Notification:
  - Topic consumers produce/consume events; email sending via Brevo non-blocking

## 8. Những rủi ro chính & phương án giảm thiểu

- Rủi ro 1: Secrets bị commit
  - Hành động: Loại bỏ keys trong git history nếu có, dùng env vars/secrets manager.
- Rủi ro 2: Consumer gây mất dữ liệu do ack sai
  - Hành động: Thêm retry/backoff, DLQ và idempotency trong producer/consumer.
- Rủi ro 3: Thiếu test/integration
  - Hành động: Prioritize unit/integration tests cho các luồng chính.
- Rủi ro 4: Hiệu năng/gánh nặng cho gateway
  - Hành động: Load test gateway; nếu cần, cân nhắc offload static proxy (Traefik/NGINX) hoặc move to Cloud Gateway product.

## 9. Các tài liệu / artifacts đã có

- `forum-kma.postman_collection.json` — Collection cho Postman (đã cập nhật các endpoint session)
- README + docs (đã di chuyển lên root)
- Source code modules: `auth-service`, `post-service`, `notification-service`, `api-gateway`, `common`, `eureka-server`.

## 10. Lời khuyên để trình bày buổi báo cáo

1. Bắt đầu bằng một slide ngắn về kiến trúc tổng (box diagram): Gateway, Auth, Post, Comment, Notification, Eureka, Kafka, DB, Redis.
2. Nói rõ công nghệ chính (và vì sao chọn Spring WebFlux + reactor): lợi ích reactive khi dùng Kafka/R2DBC/Redis.
3. Trình bày các tính năng đã hoàn thành với bằng chứng:
   - Mở Postman và demo: register -> login -> create post -> show email gửi thành công -> show sessions endpoint.
4. Nêu những việc đang làm & backlog (file service, chat, search, Rasa), plus timeline đề xuất.
5. Kết thúc bằng rủi ro & kế hoạch giảm thiểu, kèm request resources (ví dụ: time, testing infra, account Brevo nếu cần). 

## 11. Tập lệnh hữu ích & lệnh build nhanh

Build từng module (PowerShell):
```
.\gradlew.bat :auth-service:build -x test
.\gradlew.bat :post-service:build -x test
.\gradlew.bat :notification-service:build -x test
.\gradlew.bat :api-gateway:build -x test
```

Import `forum-kma.postman_collection.json` vào Postman; thêm environment variables: `gateway`, `auth`, `accessToken`, `refreshToken`.

---

Nếu bạn muốn, tôi có thể:
- Tạo slide mẫu (PowerPoint/Google Slides) dựa trên nội dung này.
- Chuẩn bị kịch bản demo step-by-step kèm screenshot/console commands.
- Chạy build cho `auth-service` và `notification-service` để đảm bảo mọi thứ compile (hiện trạng có 1 số task chưa build-verified).

Kết luận: file này đủ chi tiết để bạn trình bày buổi báo cáo đầu tiên, nêu rõ công nghệ, những gì đã xong, và roadmap tiếp theo.
