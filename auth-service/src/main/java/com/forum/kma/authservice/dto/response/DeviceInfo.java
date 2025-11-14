package com.forum.kma.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    private String ipAddress;
    private String userAgent;
    private String deviceType;
}
