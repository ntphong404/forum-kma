package com.forum.kma.postservice.repository;

import com.forum.kma.postservice.model.Comment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface CommentRepository extends ReactiveCrudRepository<Comment, String> {

    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Comment> findByPostIdPaged(String postId, long limit, long offset);
}

