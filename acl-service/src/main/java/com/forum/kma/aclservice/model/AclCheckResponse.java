package com.forum.kma.aclservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AclCheckResponse {
    private boolean allowed;
    private String reason;
}
