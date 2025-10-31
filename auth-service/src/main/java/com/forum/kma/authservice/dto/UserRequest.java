package com.forum.kma.authservice.dto;

import lombok.Data;

@Data
public class UserRequest {
  private String username;
  private String password;
  private String email;
  private String roleId;
}
