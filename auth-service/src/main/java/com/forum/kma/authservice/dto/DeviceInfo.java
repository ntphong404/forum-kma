package com.forum.kma.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceInfo {
    private String ipAddress;
    private String userAgent;
    private String deviceType; // "Mobile", "Desktop", hoặc "Unknown"
}
