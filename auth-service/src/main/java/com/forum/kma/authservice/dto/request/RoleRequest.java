package com.forum.kma.authservice.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class RoleRequest {
  private String name;
  private Set<String> permissions; // danh sách tên permission
}
