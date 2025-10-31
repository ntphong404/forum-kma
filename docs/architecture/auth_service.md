# Auth Service

## Chức năng
- Đăng ký, đăng nhập, sinh JWT access/refresh token.
- Xác thực token, cung cấp endpoint refresh token.
- Quản lý user, role, phân quyền cơ bản (RBAC).

## Công nghệ
- Spring Boot WebFlux, R2DBC, PostgreSQL.
- Sử dụng thư viện JWT chung (common module).

## Luồng hoạt động
1. Nhận thông tin đăng nhập từ client.
2. Kiểm tra thông tin, sinh JWT trả về.
3. Xác thực JWT khi có request cần bảo vệ.
4. Hỗ trợ refresh token.
