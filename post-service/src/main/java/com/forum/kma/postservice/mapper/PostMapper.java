package com.forum.kma.postservice.mapper;

import com.forum.kma.postservice.dto.CreatePostRequest;
import com.forum.kma.postservice.dto.PostResponse;
import com.forum.kma.postservice.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    Post toEntity(CreatePostRequest req);

    PostResponse toDto(Post post);
}
