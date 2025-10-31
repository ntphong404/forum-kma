# ACL Service

## Chức năng
- Kiểm soát phân quyền chi tiết đến từng resource (bài viết, comment...)
- Nhận request kiểm tra quyền từ các service khác (post, comment...)
- Trả về quyết định cho phép/từ chối dựa trên rule động.

## Công nghệ
- Spring Boot WebFlux, R2DBC, PostgreSQL.
- Có thể tích hợp OPA (Open Policy Agent) hoặc custom rule engine.

## Luồng hoạt động
1. Service gửi request kiểm tra quyền (user, action, resource...)
2. ACL Service kiểm tra rule, trả về quyết định (granted/denied).
