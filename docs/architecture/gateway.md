# Gateway Service

## Vai trò
- Là entrypoint duy nhất cho toàn bộ hệ thống.
- Xác thực JWT, reject request không hợp lệ.
- Định tuyến request đến các service nội bộ (auth, post, ...).
- Có thể thực hiện logging, rate limiting, caching...

## Công nghệ
- Spring Cloud Gateway (WebFlux, reactive, non-blocking)
- Tích hợp với Eureka để tự động phát hiện service.

## Luồng xử lý
1. Nhận request từ client.
2. Kiểm tra JWT (nếu cần), reject nếu không hợp lệ.
3. Forward request đến service tương ứng.
4. Nhận response, trả về client.
