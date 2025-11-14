package com.forum.kma.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type;          // "NEW_COMMENT", "NEW_POST", "NEW_CHAT_MESSAGE"
    private String targetUserId;  // User ID sẽ nhận thông báo
    private String triggerUserId; // User ID gây ra sự kiện
    private String triggerUsername; // Tên user gây ra sự kiện (để hiển thị)
    private String resourceUrl;   // Link đến bài post/comment
    private String message;       // Nội dung thông báo
}