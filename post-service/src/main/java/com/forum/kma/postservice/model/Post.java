package com.forum.kma.postservice.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("posts")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post implements Persistable<String> {

    @Id
    @Builder.Default
    String postId = UUID.randomUUID().toString();

    String title;

    String content;

    String authorId;

    Status status;

    int reactionCount;

    // Thêm trường này để R2DBC biết đây là entity MỚI
    @Transient // Không lưu vào DB
    @Builder.Default
    boolean isNew = true; // Mặc định là mới khi tạo qua Builder

    @Override
    public String getId() {
        return this.postId;
    }

    @Override
    @Transient
    public boolean isNew() {
        // R2DBC sẽ gọi hàm này. Nếu true -> INSERT, nếu false -> UPDATE
        return this.isNew;
    }

    // Bạn cần gọi hàm này sau khi save thành công trong Service
    public Post asNotNew() {
        this.isNew = false;
        return this;
    }

    public enum Status {
        DRAFT,
        PUBLISHED,
        DELETED
    }
}
