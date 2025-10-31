
# Reactive Programming (Lập trình phản ứng)

## 1. Tổng quan & Động lực
Reactive Programming là mô hình lập trình tập trung vào xử lý luồng dữ liệu bất đồng bộ (asynchronous data streams) và phản ứng với sự kiện. Thay vì "kéo" dữ liệu (pull), reactive cho phép "đẩy" dữ liệu (push) khi có sự kiện mới, giúp ứng dụng phản hồi nhanh, tiết kiệm tài nguyên và dễ mở rộng.

### Khi nào nên dùng?
- Hệ thống cần xử lý nhiều kết nối đồng thời (websocket, API, IoT...)
- Ứng dụng real-time (chat, dashboard, streaming)
- Khi muốn tận dụng tối đa tài nguyên CPU, tránh blocking thread

## 2. Kiến thức nền tảng
- **Publisher**: Nguồn phát dữ liệu (Flux, Mono)
- **Subscriber**: Đăng ký nhận dữ liệu
- **Operator**: Các hàm biến đổi, xử lý dữ liệu (map, filter, flatMap...)
- **Backpressure**: Cơ chế kiểm soát tốc độ tiêu thụ dữ liệu

### Sơ đồ reactive
```
Publisher --(data/events)--> Operator(s) --(data)--> Subscriber
```

## 3. Flux & Mono: Hai building block chính
- **Mono<T>**: Đại diện cho 0 hoặc 1 giá trị (thường dùng cho truy vấn đơn, chi tiết)
- **Flux<T>**: Đại diện cho 0...N giá trị (dòng dữ liệu, danh sách, stream)

### Các method phổ biến của Mono/Flux
| Method         | Ý nghĩa/ngữ cảnh sử dụng                  | Ví dụ code |
|---------------|-------------------------------------------|------------|
| just          | Tạo publisher từ giá trị có sẵn            | `Mono.just(1)` |
| empty         | Publisher không phát ra giá trị nào        | `Mono.empty()` |
| fromIterable  | Tạo Flux từ List/Collection                | `Flux.fromIterable(list)` |
| map           | Biến đổi từng phần tử                      | `flux.map(x -> x*2)` |
| filter        | Lọc dữ liệu theo điều kiện                 | `flux.filter(x -> x>0)` |
| flatMap       | Biến đổi bất đồng bộ, trả về publisher mới | `flux.flatMap(repo::findById)` |
| concatWith    | Nối 2 publisher                           | `flux1.concatWith(flux2)` |
| zipWith       | Kết hợp 2 publisher                        | `flux1.zipWith(flux2)` |
| doOnNext      | Thực thi side-effect khi có dữ liệu        | `flux.doOnNext(log::info)` |
| subscribe     | Đăng ký nhận dữ liệu (bắt đầu thực thi)    | `flux.subscribe(System.out::println)` |

### Các method phổ biến của Mono/Flux (giải thích & ví dụ)

#### just
Tạo publisher từ giá trị có sẵn.
```java
Mono.just(1);
Flux.just(1, 2, 3);
```

#### empty
Publisher không phát ra giá trị nào.
```java
Mono.empty();
Flux.empty();
```

#### fromIterable
Tạo Flux từ List/Collection.
```java
Flux.fromIterable(List.of("a", "b", "c"));
```

#### map
Biến đổi từng phần tử.
```java
Flux.just(1, 2, 3).map(x -> x * 2); // 2, 4, 6
```

#### filter
Lọc dữ liệu theo điều kiện.
```java
Flux.range(1, 5).filter(x -> x % 2 == 0); // 2, 4
```

#### flatMap
Biến đổi bất đồng bộ, trả về publisher mới.
```java
Flux.just("a", "b").flatMap(s -> Mono.just(s.toUpperCase()));
```

#### concatWith
Nối 2 publisher.
```java
Flux.just(1, 2).concatWith(Flux.just(3, 4)); // 1, 2, 3, 4
```

#### zip/zipWith
Kết hợp nhiều publisher, trả về tuple.
```java
Flux.zip(Flux.just(1, 2), Flux.just("A", "B"))
    .subscribe(t -> System.out.println(t.getT1() + t.getT2())); // 1A, 2B
```

#### doOnNext
Thực thi side-effect khi có dữ liệu.
```java
Flux.just("a", "b").doOnNext(System.out::println);
```

#### subscribe
Đăng ký nhận dữ liệu (bắt đầu thực thi pipeline).
```java
flux.subscribe(System.out::println);
```

#### switchIfEmpty
Nếu publisher không phát ra giá trị, chuyển sang publisher khác.
```java
Mono.empty()
    .switchIfEmpty(Mono.just("default"))
    .subscribe(System.out::println); // default
```

#### defaultIfEmpty
Nếu không có giá trị, phát ra giá trị mặc định.
```java
Flux.empty().defaultIfEmpty(0).subscribe(System.out::println); // 0
```

#### onErrorResume
Xử lý lỗi, trả về publisher khác khi gặp lỗi.
```java
Mono.error(new RuntimeException())
    .onErrorResume(e -> Mono.just(-1))
    .subscribe(System.out::println); // -1
```

#### onErrorMap
Chuyển đổi exception thành loại khác.
```java
Mono.error(new Exception("fail"))
    .onErrorMap(e -> new IllegalStateException(e.getMessage()))
    .subscribe();
```

#### take
Lấy N phần tử đầu tiên.
```java
Flux.range(1, 10).take(3); // 1, 2, 3
```

#### buffer
Gom nhóm phần tử thành List.
```java
Flux.range(1, 6).buffer(2)
    .subscribe(System.out::println); // [1,2], [3,4], [5,6]
```

#### window
Chia thành nhiều Flux nhỏ.
```java
Flux.range(1, 4).window(2)
    .flatMap(f -> f.collectList())
    .subscribe(System.out::println); // [1,2], [3,4]
```

#### delayElements
Trì hoãn phát từng phần tử.
```java
Flux.range(1, 3).delayElements(Duration.ofSeconds(1));
```

#### merge
Gộp nhiều publisher, không đảm bảo thứ tự.
```java
Flux.merge(Flux.just(1, 3), Flux.just(2, 4)); // 1, 2, 3, 4 (thứ tự có thể thay đổi)
```

#### combineLatest
Kết hợp publisher, phát ra khi có bất kỳ publisher nào phát.
```java
Flux.combineLatest(
    Flux.interval(Duration.ofMillis(100)),
    Flux.just("A", "B", "C"),
    (a, b) -> a + "-" + b
).subscribe(System.out::println);
```

#### reduce
Tích lũy các phần tử thành 1 giá trị.
```java
Flux.range(1, 4).reduce((a, b) -> a + b)
    .subscribe(System.out::println); // 10
```

#### collectList
Gom tất cả phần tử thành List.
```java
Flux.just(1, 2, 3).collectList().subscribe(System.out::println); // [1, 2, 3]
```

#### distinct
Loại bỏ phần tử trùng lặp.
```java
Flux.just(1, 2, 2, 3).distinct().subscribe(System.out::println); // 1, 2, 3
```
### Ví dụ tổng hợp
```java
Flux.just("A", "B", "C")
    .map(String::toLowerCase)
    .filter(s -> !s.equals("b"))
    .flatMap(s -> Mono.just(s + "!"))
    .subscribe(System.out::println); // a!, c!
```

## 4. Pattern & Best Practice
- **Chaining**: Kết hợp nhiều operator thành pipeline (map, filter, flatMap...)
- **Error Handling**: Luôn xử lý lỗi với `onErrorResume`, `onErrorReturn`...
- **Backpressure**: Dùng `limitRate`, `buffer`, `window` để kiểm soát tốc độ
- **Threading**: Dùng `publishOn`, `subscribeOn` để kiểm soát thread (Scheduler)

### Ví dụ error handling
```java
Mono.just("abc")
    .map(s -> Integer.parseInt(s))
    .onErrorResume(e -> Mono.just(-1))
    .subscribe(System.out::println); // -1
```

### Ví dụ backpressure
```java
Flux.range(1, 1000)
    .limitRate(100)
    .subscribe(System.out::println);
```

## 5. Ứng dụng thực tiễn
- **API Gateway**: Xử lý hàng nghìn request đồng thời mà không cần nhiều thread.
- **Microservices**: Dễ dàng kết nối, truyền dữ liệu giữa các service qua stream.
- **Streaming**: Xử lý dữ liệu liên tục (log, sensor, video...)
- **Realtime UI**: Push dữ liệu tới client qua websocket, SSE.

## 6. Công nghệ phổ biến
- **Project Reactor** (Spring WebFlux)
- **RxJava**
- **Akka Streams**
- **Vert.x**

## 7. Lưu ý khi dùng
- Debug khó hơn so với code tuần tự truyền thống.
- Không phải mọi bài toán đều phù hợp (ví dụ: xử lý batch lớn, không cần concurrency).
- Cần hiểu rõ về backpressure, error handling.
- Nên log, monitor các pipeline lớn.

## 8. Tài liệu tham khảo
- [reactivex.io](http://reactivex.io/)
- [Project Reactor](https://projectreactor.io/)
- [Spring WebFlux Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/)
