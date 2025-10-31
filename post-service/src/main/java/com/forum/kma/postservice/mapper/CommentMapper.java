package com.forum.kma.postservice.mapper;

import com.forum.kma.postservice.dto.CommentResponse;
import com.forum.kma.postservice.model.Comment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentResponse toDto(Comment comment);
}
