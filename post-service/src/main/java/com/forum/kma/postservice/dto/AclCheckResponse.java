package com.forum.kma.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AclCheckResponse {
    private boolean allowed;
    private String reason;
}
