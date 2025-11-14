# 📨 Kafka Message Queue Setup

Tài liệu này hướng dẫn cài đặt **Apache Kafka** bằng **Docker Compose** ở **KRaft mode** (không cần ZooKeeper), có **bảo mật SASL/PLAIN** và tích hợp giao diện **Kafka UI** để quản lý.

---

## 💡 Giới thiệu Kafka

**Apache Kafka** là hệ thống **Message Queue (hàng đợi thông điệp)** phân tán, dùng để:
- Gửi và nhận dữ liệu theo cơ chế **publish/subscribe**.
- Đảm bảo **tính bền vững (durability)** và **tốc độ cao** trong xử lý dữ liệu theo thời gian thực.

Kafka gồm 3 thành phần chính:

| Thành phần | Mô tả |
|-------------|-------|
| **Producer** | Gửi thông điệp (message) đến topic |
| **Broker** | Lưu trữ và phân phối message |
| **Consumer** | Nhận message từ topic |

---

## ⚙️ KRaft Mode là gì?

Từ Kafka 3.0+, ta có thể chạy Kafka **không cần ZooKeeper** nhờ **KRaft mode** (Kafka Raft metadata mode).  
Trong chế độ này:
- Kafka vừa làm **broker** (xử lý message),
- vừa làm **controller** (quản lý metadata).  
  → Giúp setup nhanh gọn, dễ dùng cho môi trường dev hoặc local.

---

## 🔒 SASL/PLAIN là gì?

Kafka hỗ trợ nhiều cơ chế bảo mật.  
Trong ví dụ này ta dùng **SASL/PLAIN**, nghĩa là:
- Xác thực bằng **username/password**.
- Dễ dùng cho môi trường phát triển (nếu muốn mã hóa nội dung → dùng SASL_SSL).

---

## 🧩 Cấu trúc dự án

```
project-root/
│
├── docker-compose.yml
├── kafka_server_jaas.conf
└── README.md
```

---

## 🪄 1️⃣ Tạo `KAFKA_CLUSTER_ID`

Kafka ở KRaft mode cần một mã định danh duy nhất cho cụm.  
Chạy lệnh sau để tạo:

```bash
docker run --rm apache/kafka:latest /opt/kafka/bin/kafka-storage.sh random-uuid
```

Ví dụ kết quả:

```
nON3ypqiQ9qGX9eFvKpcjQ
```

👉 Dán giá trị này vào biến `KAFKA_CLUSTER_ID` trong `docker-compose.yml`.

---

## 🧾 2️⃣  3️⃣ File cấu hình `kafka_server_jaas.conf`

File này định nghĩa username/password cho xác thực SASL/PLAIN:

```conf
KafkaServer {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  user_admin="2410";
};
```

---

## 🐳 3️⃣ Docker Compose

```yaml
services:
  kafka:
    image: apache/kafka:latest
    hostname: kafka
    container_name: kafka
    ports:
      - "7092:7092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CLUSTER_ID: "nON3ypqiQ9qGX9eFvKpcjQ"
      KAFKA_LISTENERS: PLAINTEXT://kafka:7092,CONTROLLER://kafka:7093,INTERNAL://kafka:7094
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:7092,INTERNAL://kafka:7094
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:7093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:SASL_PLAINTEXT,INTERNAL:SASL_PLAINTEXT
      KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
      KAFKA_OPTS: "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_NUM_PARTITIONS: 1
    volumes:
      - kafka-data:/var/lib/kafka/data
      - ./kafka_server_jaas.conf:/etc/kafka/kafka_server_jaas.conf
    networks:
      - forum-kma-net

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "7080:8080"
    restart: always
    environment:
      KAFKA_CLUSTERS_0_NAME: "horo1-kafka-secure"
      KAFKA_CLUSTERS_0_BOOTSTRAP_SERVERS: kafka:7094
      KAFKA_CLUSTERS_0_PROPERTIES_SECURITY_PROTOCOL: SASL_PLAINTEXT
      KAFKA_CLUSTERS_0_PROPERTIES_SASL_MECHANISM: PLAIN
      KAFKA_CLUSTERS_0_PROPERTIES_SASL_JAAS_CONFIG: 'org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="123456";'
    depends_on:
      - kafka
    networks:
      - forum-kma-net

volumes:
  kafka-data:

networks:
  forum-kma-net:
    external: true

```

---

## ⚙️ 4️⃣ Chạy Kafka

```bash
docker-compose up -d
docker ps
```

Truy cập giao diện Kafka UI tại:  
👉 http://localhost:7080

---

## 💻 5️⃣ Cấu hình Spring Boot để kết nối

```properties
spring.kafka.bootstrap-servers=localhost:7092
spring.kafka.properties.security.protocol=SASL_PLAINTEXT
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="2410";
spring.kafka.consumer.group-id=my-group
spring.kafka.consumer.auto-offset-reset=earliest
```

---

✅ **Hoàn tất!**  
Giờ bạn có thể dùng Kafka trong Docker với bảo mật SASL/PLAIN và giám sát trực quan qua Kafka UI.
