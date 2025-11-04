package com.forum.kma.postservice.controller;

import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.postservice.dto.CommentResponse;
import com.forum.kma.postservice.dto.CreateCommentRequest;
import com.forum.kma.postservice.mapper.CommentMapper;
import com.forum.kma.postservice.service.CommentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/comments")
public class CommentController {
    CommentService commentService;
    CommentMapper commentMapper;

    @GetMapping("/post")
    public Mono<ApiResponse<List<CommentResponse>>> getComments(
            @RequestParam("postId") String postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return commentService.getCommentsByPost(postId, page, size)
                .map(commentMapper::toDto)
                .collectList()
                .map(comments -> ApiResponse.success("Lấy danh sách bình luận thành công", comments))
                .defaultIfEmpty(ApiResponse.error(404, "Không có bình luận nào"));
    }

    @PostMapping
    public Mono<ApiResponse<CommentResponse>> createComment(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                                            @RequestBody CreateCommentRequest req) {
        if (userId == null || userId.isBlank()) {
            return Mono.just(ApiResponse.error(401, "Missing user id"));
        }

        return commentService.createComment(userId, req)
                .map(commentMapper::toDto)
                .map(c -> ApiResponse.success("Tạo bình luận thành công", c));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteComment(@PathVariable("id") String id) {
        return commentService.deleteComment(id)
                .then(Mono.just(ApiResponse.success("Xóa thành công", null)));
    }

}
