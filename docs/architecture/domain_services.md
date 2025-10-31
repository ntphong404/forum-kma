# Domain Services

## Post Service
- CRUD bài viết, bình luận.
- Áp dụng phân quyền (gọi ACL khi cần).
- Sử dụng R2DBC, Redis cache.

## User Service
- Quản lý thông tin user, profile.

## Comment Service
- CRUD bình luận, liên kết với bài viết.

## Các service khác
- Có thể mở rộng thêm: Notification, File, ...
