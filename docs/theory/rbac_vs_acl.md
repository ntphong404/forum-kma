# RBAC vs ACL: Lý thuyết & Thực tiễn

## 1. RBAC (Role-Based Access Control)
### Khái niệm
RBAC là mô hình phân quyền dựa trên vai trò. Mỗi user được gán một hoặc nhiều role, mỗi role có một tập quyền (permission) nhất định.

### Ví dụ thực tế
- Hệ thống quản lý nhân sự: Admin, Manager, Employee.
- Ứng dụng forum: ROLE_ADMIN (quản trị), ROLE_USER (người dùng), ROLE_MOD (kiểm duyệt).

### Code mẫu (Spring Security)
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String id) { ... }
```

## 2. ACL (Access Control List)
### Khái niệm
ACL là mô hình phân quyền chi tiết đến từng đối tượng/tài nguyên. Mỗi resource có một danh sách các user/role được phép thao tác (read, write, delete...).

### Ví dụ thực tế
- Google Drive: Chia sẻ file cho từng user với quyền xem/sửa/xóa riêng biệt.
- Diễn đàn: Bài viết X chỉ cho phép user A sửa, user B chỉ được xem.

### Code mẫu (giả lập)
```java
// Kiểm tra quyền sửa bài viết
if (aclService.isAllowed(userId, "edit", postId)) {
	// Cho phép sửa
}
```

## 3. So sánh tổng quan
| Tiêu chí         | RBAC                | ACL                        |
|------------------|---------------------|----------------------------|
| Đơn giản quản lý | ✔                   | ✘ (phức tạp khi lớn)       |
| Linh hoạt        | ✘                   | ✔ (tùy biến từng resource) |
| Hiệu năng tra cứu| ✔                   | ✘ (nhiều resource lớn)     |
| Phù hợp          | Doanh nghiệp, tổ chức| Hệ thống chia sẻ, cộng đồng|

## 4. Kết hợp RBAC & ACL
Nhiều hệ thống hiện đại kết hợp cả hai: RBAC cho quyền tổng quát, ACL cho ngoại lệ chi tiết.

## 5. Lưu ý triển khai
- RBAC dễ mở rộng, bảo trì, phù hợp hệ thống lớn, ít thay đổi quyền động.
- ACL phù hợp hệ thống cần kiểm soát chi tiết, nhưng phức tạp khi số lượng resource lớn.

## 6. Tham khảo
- [Spring Security ACL](https://docs.spring.io/spring-security/reference/servlet/authorization/acl.html)
- [RBAC vs ACL - StackOverflow](https://stackoverflow.com/questions/4044524/what-is-the-difference-between-acl-and-rbac)
