package com.forum.kma.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
  private String id;
  private String username;
  private String email;
  private String roleId;
}
