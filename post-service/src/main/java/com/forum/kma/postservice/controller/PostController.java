package com.forum.kma.postservice.controller;

import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.postservice.dto.CreatePostRequest;
import com.forum.kma.postservice.dto.PageResponse;
import com.forum.kma.postservice.dto.PostResponse;
import com.forum.kma.postservice.dto.UpdatePostRequest;
import com.forum.kma.postservice.mapper.PostMapper;
import com.forum.kma.postservice.model.Post;
import com.forum.kma.postservice.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/posts")
public class PostController {
    PostService postService;
    PostMapper postMapper;

    @GetMapping
    public Mono<ApiResponse<PageResponse<PostResponse>>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "search", defaultValue = "") String search
    ) {
        Mono<Long> totalMono = postService.countAll(search); // cần count tổng số bản ghi matching search

        Flux<Post> postsFlux = postService.getAll(page, limit, search);

        return postsFlux
                .map(postMapper::toDto)
                .collectList()
                .zipWith(totalMono) // kết hợp list + tổng số
                .map(tuple -> {
                    var list = tuple.getT1();
                    long total = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) total / limit);

                    return ApiResponse.success(
                            "Get all posts successfully",
                            new PageResponse<>(list, page, limit, total, totalPages)
                    );
                });
    }

    @PostMapping
    public Mono<ApiResponse<PostResponse>> createPost(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                      @RequestBody CreatePostRequest req) {
        // If gateway forwards X-User-Id header, use it; otherwise unauthenticated
        if (userId == null || userId.isBlank()) {
            return Mono.just(ApiResponse.error(401, "Missing user id"));
        }

        return postService.createPost(userId, req)
                .map(postMapper::toDto)
                .map(p -> ApiResponse.success("Tạo bài viết thành công", p));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<PostResponse>> getPost(@PathVariable("id") String id) {
        return postService.getPostById(id)
                .map(postMapper::toDto)
                .map(p -> ApiResponse.success("Lấy bài viết thành công", p))
                .defaultIfEmpty(ApiResponse.error(404, "Không tìm thấy bài viết"));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<PostResponse>> updatePost(@PathVariable("id") String id,
                                                      @RequestBody UpdatePostRequest req) {
        return postService.updatePost(id, req)
                .map(postMapper::toDto)
                .map(p -> ApiResponse.success("Cập nhật thành công", p))
                .defaultIfEmpty(ApiResponse.error(404, "Không tìm thấy bài viết"));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deletePost(@PathVariable("id") String id) {
        return postService.deletePostById(id)
                .then(Mono.just(ApiResponse.success("Xóa thành công", null)));
    }

    @GetMapping("/author/{id}")
    public Mono<ApiResponse<PageResponse<PostResponse>>> listByAuthor(
            @PathVariable("id") String authorId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "search", defaultValue = "") String search
    ) {
//
        Mono<Long> totalMono = postService.countByAuthorId(authorId,search);
        Flux<Post> postsFlux = postService.listByAuthor(authorId,page,limit, search); // 1. Flux<Post>
        return postsFlux
                .map(postMapper::toDto)                  // 2. Flux<PostResponse>
                .collectList()                           // 3. Mono<List<PostResponse>> (Gom lại)
                .zipWith(totalMono)
                .map(tuple -> {
                    var list = tuple.getT1();
                    long total = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) total / limit);

                    return ApiResponse.success(
                            "Get posts by author id successfully",
                            new PageResponse<>(list, page, limit, total, totalPages)
                    );
                });
    }
}
