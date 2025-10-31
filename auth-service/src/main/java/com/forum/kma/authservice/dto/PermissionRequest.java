package com.forum.kma.authservice.dto;

import lombok.Data;

@Data
public class PermissionRequest {
  private String name;
  private String description;
}
