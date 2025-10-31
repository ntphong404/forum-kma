# 🧠 Forum KMA — Microservice Reactive System

## 📂 Cây thư mục dự án (rút gọn)

```
forum-kma/
├── api-gateway/
│   ├── build.gradle.kts
│   └── src/
├── auth-service/
│   ├── build.gradle.kts
│   └── src/
├── common/
│   ├── build.gradle.kts
│   └── src/
├── eureka-server/
│   ├── build.gradle.kts
│   └── src/
├── post-service/
│   ├── build.gradle.kts
│   ├── src/
│   │   └── main/
│   │       └── java/com/forum/kma/postservice/
│   │           ├── controller/
│   │           ├── dto/
│   │           ├── exception/
│   │           ├── mapper/
│   │           ├── model/
│   │           ├── repository/
│   │           ├── service/
│   │           └── config/
│   │       └── resources/
│   └── test/
├── docs/
│   ├── theory/
│   │   ├── reactive.md
│   │   ├── rbac_vs_acl.md
│   │   ├── gradle_vs_maven.md
│   │   └── caching_redis.md
│   └── architecture/
│       ├── system_architecture.md
│       ├── gateway.md
│       ├── auth_service.md
│       ├── acl_service.md
│       └── domain_services.md
└── ...
```

---

## 📘 Tổng quan

**Forum KMA** là hệ thống diễn đàn được thiết kế theo kiến trúc **Microservice Reactive**, với các thành phần chính:
- Gateway
- Eureka (Service Discovery)
- Auth & ACL Service
- Các domain service (Post, Chat, File, Mail…)
- Redis & Kafka
- Prometheus, Grafana, ELK Stack cho quan sát

---

## 🏗️ Kiến trúc hệ thống

> 🔗 [Xem chi tiết tại đây](docs/architecture/system_architecture.md)

Tổng quan hệ thống bao gồm các thành phần:
- **Gateway Service**: định tuyến và xác thực JWT.
- **Eureka Service**: quản lý service discovery.
- **Auth Service**: quản lý user, RBAC, token.
- **ACL Service**: quản lý quyền chi tiết.
- **Các Domain Service**: Post, Chat, Notification...
- **Redis**, **Kafka**, **MongoDB**, **PostgreSQL** hỗ trợ dữ liệu và event.

---

## 🧠 Lý thuyết nền tảng

| Chủ đề | Tài liệu |
|--------|-----------|
| Reactive Programming | [📘 Reactive Programming](docs/theory/reactive.md) |
| RBAC & ACL | [🔐 RBAC vs ACL](docs/theory/rbac_vs_acl.md) |
| Gradle vs Maven | [⚙️ Gradle vs Maven](docs/theory/gradle_vs_maven.md) |
| Redis Caching | [💾 Redis Caching](docs/theory/caching_redis.md) |

---

## ⚡ Mạng Docker

Tất cả các service cần nằm trong cùng một network:

```bash
docker network create forum-kma-net
```
Trong docker-compose.yml của mỗi service:
```
networks:
    forum-kma-net:
        external: true
```
