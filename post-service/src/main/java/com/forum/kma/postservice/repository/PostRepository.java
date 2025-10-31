package com.forum.kma.postservice.repository;

import com.forum.kma.postservice.model.Post;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, String> {

    @Query("""
        SELECT * FROM posts
        WHERE author_id = :authorId
          AND LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY title ASC
    """)
    Flux<Post> findByAuthorIdAndTitleContaining(String authorId, String search);
}
