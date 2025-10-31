package com.forum.kma.postservice.service;

import com.forum.kma.postservice.model.Comment;
import com.forum.kma.postservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

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

    public Mono<Comment> createComment(String authorId, com.forum.kma.postservice.dto.CreateCommentRequest req) {
        Comment comment = Comment.builder()
                .postId(req.postId())
                .authorId(authorId)
                .content(req.content())
                .reactionCount(0)
                .build();

        return commentRepository.save(comment);
    }
}
