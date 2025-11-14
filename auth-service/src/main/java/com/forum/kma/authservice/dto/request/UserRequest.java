package com.forum.kma.authservice.dto.request;

import lombok.Data;

@Data
public class UserRequest {
  private String username;
  private String password;
  private String email;
  private String roleId;
  private String userStatus; // optional: PENDING, ACTIVE, BANNED
  private Boolean is2FAEnabled;
}
