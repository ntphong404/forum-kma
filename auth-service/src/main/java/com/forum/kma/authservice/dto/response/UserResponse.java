package com.forum.kma.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
  private String id;
  private String username;
  private String email;
  private String roleId;
  private String userStatus;
  private Boolean is2FAEnabled;
}
