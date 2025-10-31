# Caching & Redis: Lý thuyết, thực tiễn, pattern và ví dụ nâng cao

## 1. Caching là gì? Tại sao cần cache?
Caching là kỹ thuật lưu trữ tạm thời dữ liệu truy xuất thường xuyên vào bộ nhớ nhanh (RAM, Redis, Memcached...) để tăng tốc độ truy xuất, giảm tải backend/database, cải thiện trải nghiệm người dùng và tiết kiệm chi phí hạ tầng.

### Lợi ích
- Giảm độ trễ (latency) khi truy xuất dữ liệu
- Giảm số lượng truy vấn DB, tiết kiệm tài nguyên
- Tăng khả năng chịu tải (scalability)

### Ví dụ thực tế
- Website tin tức: Cache trang chủ, danh sách bài viết để giảm truy vấn DB.
- API: Cache kết quả truy vấn phổ biến (top 10 bài viết, trending...)
- E-commerce: Cache thông tin sản phẩm, giá, tồn kho

## 2. Redis là gì? Vì sao Redis phổ biến?
- Redis là hệ quản trị cơ sở dữ liệu NoSQL dạng key-value, lưu trữ trên RAM, tốc độ rất cao (microseconds).
- Hỗ trợ nhiều kiểu dữ liệu: String, Hash, List, Set, Sorted Set, Stream...
- Có thể dùng làm cache, message broker, pub/sub, queue, rate limiter...

### Ưu điểm khi dùng Redis làm cache
- Truy xuất cực nhanh (so với DB truyền thống)
- Hỗ trợ TTL (tự động hết hạn cache)
- Dễ tích hợp với Spring, Node.js, Python...
- Hỗ trợ cluster, replication, persistence (AOF, RDB)

## 3. Các pattern caching phổ biến
- **Read-through cache**: Khi truy vấn, nếu cache miss thì lấy từ DB và lưu vào cache.
- **Write-through cache**: Khi ghi dữ liệu, ghi vào cache và DB đồng thời.
- **Cache aside (Lazy loading)**: Ứng dụng tự kiểm tra cache, nếu miss thì lấy từ DB và update cache.
- **Write-back (Write-behind)**: Ghi vào cache trước, sau đó đồng bộ sang DB sau.

### Minh họa pattern cache-aside (Spring Boot)
```java
@Cacheable("posts")
public Post getPost(String id) {
	// Nếu cache miss, sẽ truy vấn DB và lưu vào cache
}

@CacheEvict(value = "posts", key = "#id")
public void deletePost(String id) {
	// Xóa cache khi xóa bài viết
}
```

## 4. Code mẫu nâng cao với RedisTemplate
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void saveToCache(String key, Object value) {
	redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
}

public Object getFromCache(String key) {
	return redisTemplate.opsForValue().get(key);
}
```

## 5. Best Practice khi dùng cache
- Chọn TTL hợp lý để tránh dữ liệu cũ (stale data)
- Invalidate cache đúng lúc (khi dữ liệu thay đổi)
- Không cache dữ liệu quá nhạy cảm hoặc cá nhân hóa
- Đặt key cache có prefix rõ ràng ("user:123", "post:456")
- Giám sát (monitor) tỉ lệ hit/miss để tối ưu

## 6. Anti-pattern cần tránh
- Cache stampede: Nhiều request cùng lúc cache miss, cùng truy vấn DB (giải pháp: lock, random TTL)
- Cache avalanche: Hết TTL đồng loạt, hệ thống bị "dội bom" DB
- Cache pollution: Cache dữ liệu ít truy cập, lãng phí bộ nhớ

## 7. Tham khảo
- [Redis.io](https://redis.io/)
- [Spring Cache Docs](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Caching Patterns - Microsoft Docs](https://learn.microsoft.com/en-us/azure/architecture/best-practices/caching)
