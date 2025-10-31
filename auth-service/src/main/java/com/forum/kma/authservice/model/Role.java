package com.forum.kma.authservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Set;
import java.util.UUID;

@Document(collection = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
  @Id
  @Builder.Default
  private String id = UUID.randomUUID().toString();
  private String name;
  private Set<String> permissions; // Danh sách tên permission (name)
}
