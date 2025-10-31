------------------------------------------------------------
-- 🧾 BẢNG POSTS (Bài đăng)
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    -- Khóa chính (UUID lưu dạng String)
    post_id VARCHAR(255) PRIMARY KEY,

    -- Tiêu đề và nội dung bài viết
    title TEXT NOT NULL,
    content TEXT,

    -- ID tác giả
    author_id VARCHAR(255) NOT NULL,

    -- Trạng thái bài đăng: DRAFT, PUBLISHED, DELETED
    status VARCHAR(50) NOT NULL,

    -- Số lượng cảm xúc (reaction)
    reaction_count INT DEFAULT 0,

    -- Thời gian tạo và cập nhật
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

------------------------------------------------------------
-- 💬 BẢNG COMMENTS (Bình luận)
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    -- Khóa chính
    comment_id VARCHAR(255) PRIMARY KEY,

    -- Liên kết với bài đăng
    post_id VARCHAR(255) NOT NULL,

    -- ID người bình luận
    author_id VARCHAR(255) NOT NULL,

    -- Nội dung bình luận
    content TEXT NOT NULL,

    -- Thời gian tạo và cập nhật
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Số lượng cảm xúc (reaction)
    reaction_count INT DEFAULT 0,

    -- Ràng buộc khóa ngoại
    CONSTRAINT fk_post
    FOREIGN KEY (post_id)
    REFERENCES posts (post_id)
                         ON DELETE CASCADE
    );