package com.forum.kma.postservice.service;

import com.forum.kma.postservice.dto.CreateCommentRequest;
import com.forum.kma.postservice.model.Comment;
import com.forum.kma.postservice.repository.CommentRepository;
import com.forum.kma.postservice.repository.PostRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {

    CommentRepository commentRepository;
    PostRepository postRepository;

    public Flux<Comment> getCommentsByPost(String postId, int page, int size) {
        long offset = (long) page * size;
        return commentRepository.findByPostIdPaged(postId, size, offset);
    }

    public Mono<Comment> addComment(Comment comment) {
        return commentRepository.save(comment);
    }

    public Mono<Void> deleteComment(String id) {
        return commentRepository.deleteById(id);
    }

    public Mono<Comment> createComment(String authorId, CreateCommentRequest req) {
        return postRepository.findById(req.postId())
                .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                .flatMap(post -> {
                    Comment comment = Comment.builder()
                            .postId(req.postId())
                            .authorId(authorId)
                            .content(req.content())
                            .reactionCount(0)
                            .build();

                    return commentRepository.save(comment);
                });
    }

}
