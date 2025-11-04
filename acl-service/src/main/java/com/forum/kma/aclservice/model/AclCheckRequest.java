package com.forum.kma.aclservice.model;

import lombok.Data;

@Data
public class AclCheckRequest {
    private String type;       // ví dụ: "POST", "CHAT"
    private String objectId;   // id của tài nguyên
    private String authorId;   // id của người tạo
    private String userId;     // id của người yêu cầu
    private String action;     // hành động ("READ", "UPDATE", "DELETE")
    private String roleName;
}
