package com.forum.kma.authservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String username;
    private String password;
    private String email;

    private String roleId; // Lưu id hoặc name của role
}