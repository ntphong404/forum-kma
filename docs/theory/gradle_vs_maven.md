# So sánh chi tiết Gradle và Maven

## 1. Tổng quan
**Maven** và **Gradle** là hai công cụ build phổ biến nhất cho Java ecosystem.

## 2. Cách cấu hình
- **Maven**: Dùng XML (`pom.xml`), cấu trúc cứng nhắc, dễ đọc, nhiều tài liệu.
- **Gradle**: Dùng Groovy/Kotlin DSL (`build.gradle`, `build.gradle.kts`), cú pháp linh hoạt, hỗ trợ scripting mạnh.

### Ví dụ cấu hình dependency
**Maven:**
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Gradle:**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
```

## 3. Tốc độ build
- Gradle nhanh hơn nhờ cơ chế cache thông minh, build incremental.
- Maven chậm hơn, nhưng ổn định.

## 4. Plugin & mở rộng
- Gradle: Hệ plugin phong phú, dễ viết plugin custom.
- Maven: Plugin nhiều, nhưng cấu hình cứng nhắc hơn.

## 5. Khi nào chọn gì?
- **Gradle**: Dự án lớn, CI/CD, build phức tạp, cần scripting.
- **Maven**: Dự án nhỏ, cần sự ổn định, team mới học Java.

## 6. Thực tiễn
- Hầu hết dự án Spring Boot hiện đại đều hỗ trợ cả hai.
- Nhiều công ty lớn chuyển dần sang Gradle để tối ưu CI/CD.

## 7. Tài liệu tham khảo
- [Gradle vs Maven - Baeldung](https://www.baeldung.com/gradle-vs-maven)
- [Gradle Official](https://gradle.org/)
- [Maven Official](https://maven.apache.org/)
