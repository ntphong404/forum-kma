package com.forum.kma.postservice.service;

import com.forum.kma.common.exception.AppException;
import com.forum.kma.postservice.dto.UpdatePostRequest;
import com.forum.kma.postservice.exception.PostErrorCode;
import com.forum.kma.postservice.model.Post;
import com.forum.kma.postservice.repository.PostRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PostService {
    PostRepository postRepository;

    public Flux<Post> getAll(int page, int limit, String search) {
        int offset = page * limit;
        return postRepository.findAllPostWithPagination(search, offset, limit);
    }

    public Mono<Post> updatePost(String postId, UpdatePostRequest request) {
        return postRepository.findById(postId)
                .flatMap(existingPost -> {
                    // cập nhật nếu có giá trị mới
                    if (request.newTitle() != null && !request.newTitle().isBlank()) {
                        existingPost.setTitle(request.newTitle());
                    }
                    if (request.newContent() != null && !request.newContent().isBlank()) {
                        existingPost.setContent(request.newContent());
                    }
                    existingPost.asNotNew();
                    return postRepository.save(existingPost);
                })
                .switchIfEmpty(Mono.error(new AppException(PostErrorCode.RESOURCE_NOT_FOUND)));
    }

    public Mono<Post> createPost(String authorId, com.forum.kma.postservice.dto.CreatePostRequest req) {
        Post post = Post.builder()
                .title(req.title())
                .content(req.content())
                .authorId(authorId)
                .status(Post.Status.DRAFT)
                .reactionCount(0)
                .build();

        return postRepository.save(post).map(Post::asNotNew);
    }

    public Mono<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }

    public Mono<Void> deletePostById(String postId) {
        return postRepository.deleteById(postId);
    }

    public Flux<Post> listByAuthor(String authorId,int page, int limit, String search) {
        int offset = page * limit;
        return postRepository.findByAuthorIdAndTitleContaining(authorId, offset, limit, search);
    }

    public Mono<Long> countAll(String search) {
        return postRepository.countWithSearch(search);
    }

    public  Mono<Long> countByAuthorId(String authorId,String search) {
        return postRepository.countByAuthorIdAndSearch(authorId,search);
    }
}
