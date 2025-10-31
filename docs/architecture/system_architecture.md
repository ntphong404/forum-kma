# Kiến trúc hệ thống tổng thể

## Sơ đồ tổng quan
```
[Client] <-> [API Gateway] <-> [Auth Service, Post Service, ...] <-> [DB, Redis]
                                 |
                                 +-- [ACL Service]
                                 |
                                 +-- [Eureka Server]
```

- **API Gateway**: Cửa ngõ duy nhất, xác thực JWT, định tuyến request.
- **Auth Service**: Đăng nhập, đăng ký, sinh và xác thực token.
- **Post Service**: Quản lý bài viết, bình luận.
- **ACL Service**: Kiểm soát phân quyền chi tiết (RBAC/ACL).
- **Eureka Server**: Service discovery.
- **Redis**: Cache, tăng tốc truy xuất.

## Luồng hoạt động
1. Client gửi request qua Gateway (có JWT nếu đã đăng nhập).
2. Gateway xác thực token, forward request đến service phù hợp.
3. Service xử lý nghiệp vụ, có thể gọi ACL để kiểm tra quyền.
4. Kết quả trả về client qua Gateway.
