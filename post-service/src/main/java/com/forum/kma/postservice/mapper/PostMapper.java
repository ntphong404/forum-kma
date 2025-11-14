package com.forum.kma.postservice.mapper;

import com.forum.kma.common.event.PostEvent;
import com.forum.kma.postservice.dto.CreatePostRequest;
import com.forum.kma.postservice.dto.PostResponse;
import com.forum.kma.postservice.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface PostMapper {

    Post toEntity(CreatePostRequest req);

    PostResponse toDto(Post post);

    @Mapping(source = "postId", target = "id")
    PostEvent toEvent(Post post);
}
