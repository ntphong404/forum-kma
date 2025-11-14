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

        // === 1. Define Permissions ===
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

        Set<String> guestPermissions = Set.of();

        // === 2. Create or find roles ===
        Mono<Role> adminRoleMono = createRoleIfNotExist("ADMIN", adminPermissions);
        Mono<Role> userRoleMono = createRoleIfNotExist("USER", userPermissions);
        Mono<Role> guestRoleMono = createRoleIfNotExist("GUEST", guestPermissions);

        // === 3. Combine all roles & ensure admin user exists ===
        Mono.zip(adminRoleMono, userRoleMono, guestRoleMono)
                .flatMap(tuple -> {
                    Role adminRole = tuple.getT1();

                    // Create admin user if missing
                    return userRepository.findByUsername("admin")
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Creating admin user...");
                                return userRepository.insert(User.builder()
                                                .username("admin")
                                                .email("admin@forumkma.com")
                                                .password(passwordEncoder.encode("admin123"))
                                                .roleId(adminRole.getId())
                                                .is2FAEnabled(false)
                                                .userStatus(User.UserStatus.ACTIVE)
                                                .build())
                                        .onErrorMap(DuplicateKeyException.class,
                                                ex -> new AppException(AuthErrorCode.DATABASE_SAVE_FAILED));
                            }));
                })
                .subscribe(
                        user -> log.info("✅ Database initialization successful. Admin user ready: {}", user.getUsername()),
                        error -> log.error("❌ Database initialization failed: {}", error.getMessage())
                );
    }

    private Mono<Role> createRoleIfNotExist(String name, Set<String> permissions) {
        log.info("Create role {} if not exist", name);
        return roleRepository.findByName(name)
                .switchIfEmpty(Mono.defer(() ->
                        roleRepository.insert(Role.builder()
                                        .name(name)
                                        .permissions(permissions)
                                        .build())
                                .doOnSuccess(role -> log.info("Created role with name: {}", role.getName()))
                                .onErrorMap(DuplicateKeyException.class, ex -> new AppException(AuthErrorCode.DATABASE_SAVE_FAILED))
                ));
    }

}
