package com.forum.kma.postservice.controller;

import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.postservice.dto.CreatePostRequest;
import com.forum.kma.postservice.dto.PostResponse;
import com.forum.kma.postservice.dto.UpdatePostRequest;
import com.forum.kma.postservice.mapper.PostMapper;
import com.forum.kma.postservice.model.Post;
import com.forum.kma.postservice.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/posts")
public class PostController {
    PostService postService;
    PostMapper postMapper;

    @PostMapping
    public Mono<ApiResponse<PostResponse>> createPost(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                      @RequestBody CreatePostRequest req) {
        // If gateway forwards X-User-Id header, use it; otherwise unauthenticated
        log.info("createPost, userId={}, req={}", userId, req);
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
    public Mono<ApiResponse<List<PostResponse>>> listByAuthor(@PathVariable("id") String authorId,
                                                              @RequestParam(name = "search", defaultValue = "") String search) {

        return postService.listByAuthor(authorId, search) // 1. Flux<Post>
                .map(postMapper::toDto)                  // 2. Flux<PostResponse>
                .collectList()                           // 3. Mono<List<PostResponse>> (Gom lại)
                .map(list -> ApiResponse.success("Lấy thành công danh sách post", list)); // 4. Mono<ApiResponse<List<PostResponse>>>
    }
}
