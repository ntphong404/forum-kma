package com.forum.kma.authservice.config;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.constant.Permission;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.model.Role;
import com.forum.kma.authservice.repository.UserRepository;
import com.forum.kma.authservice.repository.RoleRepository;
import com.forum.kma.common.exception.AppException;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppStartupRunner implements CommandLineRunner {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    log.info("Starting database initialization...");

    // 1. Định nghĩa Permissions (sử dụng Enum Permission đã đổi tên)
    Set<String> adminPermissions = Permission.getPermissionsAsStrings(
            Permission.USER_CREATE,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE,

            Permission.ROLE_MANAGEMENT,

            Permission.POST_CREATE,
            Permission.POST_READ,
            Permission.POST_DELETE,
            Permission.CONFIG_ACCESS
    );
    Set<String> userPermissions = Permission.getPermissionsAsStrings(
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE,

            Permission.POST_CREATE,
            Permission.POST_READ,
            Permission.POST_UPDATE,
            Permission.POST_DELETE
    );


    // 2. Tạo hoặc Tìm kiếm Admin Role và User Role
    Mono<Role> adminRoleMono = roleRepository.findByName("ADMIN")
            .switchIfEmpty(Mono.defer(() -> {
              log.info("Creating ADMIN role...");
              return roleRepository.insert(Role.builder()
                      .name("ADMIN")
                      .permissions(adminPermissions)
                      .build())
                      .onErrorMap(DuplicateKeyException.class, ex -> new AppException(AuthErrorCode.SOMETHING_WRONG));
            }));

    Mono<Role> userRoleMono = roleRepository.findByName("USER")
            .switchIfEmpty(Mono.defer(() -> {
              log.info("Creating USER role...");
              return roleRepository.insert(Role.builder()
                      .name("USER")
                      .permissions(userPermissions)
                      .build())
                      .onErrorMap(DuplicateKeyException.class, ex -> new AppException(AuthErrorCode.SOMETHING_WRONG));
            }));


    // 3. Kết hợp 2 Mono Role và tạo Admin User
    Mono.zip(adminRoleMono, userRoleMono)
            .flatMap(tuple -> {
              Role adminRole = tuple.getT1();
              Role userRole = tuple.getT2();

               log.info("Admin Role ID: {}", adminRole.getId());
              log.info("User Role ID: {}", userRole.getId());

              // Tạo user admin nếu chưa có, sử dụng Admin Role ID
              return userRepository.findByUsername("admin")
                      .switchIfEmpty(Mono.defer(() -> {
                        log.info("Creating admin user...");
                        return userRepository.insert(User.builder()
                                .username("admin")
                                .email("admin@forumkma.com")
                                .password(passwordEncoder.encode("admin123"))
                                .roleId(adminRole.getId()) // Lấy ID của Admin Role
                                .build())
                                .onErrorMap(DuplicateKeyException.class, ex -> new AppException(AuthErrorCode.SOMETHING_WRONG));
                      }));
            })
            .subscribe(
                    user -> log.info("✅ Database initialization successful. Admin user ready: {}", user.getUsername()),
                    error -> log.error("❌ Database initialization failed: {}", error.getMessage())
            );
  }
}