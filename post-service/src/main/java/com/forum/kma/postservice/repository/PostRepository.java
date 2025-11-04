package com.forum.kma.postservice.repository;

import com.forum.kma.postservice.model.Post;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, String> {

    @Query("""
        SELECT * FROM posts
        WHERE author_id = :authorId
          AND LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY title ASC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
    """)
    Flux<Post> findByAuthorIdAndTitleContaining(String authorId,int offset, int limit, String search);

    @Query("""
    SELECT * FROM posts
    WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
    ORDER BY title ASC
    OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
""")
    Flux<Post> findAllPostWithPagination(String search, int offset, int limit);

    @Query("""
    SELECT COUNT(*) FROM posts
     WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Mono<Long> countWithSearch(String search);

    @Query("""
    SELECT COUNT(*) FROM posts
    WHERE LOWER(author_id) LIKE LOWER(CONCAT('%', :authorId, '%'))
      AND LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Mono<Long> countByAuthorIdAndSearch(String authorId, String search);

}
