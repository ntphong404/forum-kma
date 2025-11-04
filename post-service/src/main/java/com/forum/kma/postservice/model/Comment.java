package com.forum.kma.postservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("comments")
public class Comment implements Persistable<String> {

    @Id
    @Builder.Default
    String commentId  = UUID.randomUUID().toString();

    String postId;
    String authorId;
    String content;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    int reactionCount;

    // Thêm trường này để R2DBC biết đây là entity MỚI
    @Transient // Không lưu vào DB
    @Builder.Default
    boolean isNew = true; // Mặc định là mới khi tạo qua Builder

    @Override
    public String getId() {
        return this.commentId;
    }

    @Override
    @Transient
    public boolean isNew() {
        // R2DBC sẽ gọi hàm này. Nếu true -> INSERT, nếu false -> UPDATE
        return this.isNew;
    }

    // Bạn cần gọi hàm này sau khi save thành công trong Service
    public Comment asNotNew() {
        this.isNew = false;
        return this;
    }
}
