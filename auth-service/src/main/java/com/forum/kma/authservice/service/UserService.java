package com.forum.kma.authservice.service;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.request.UserRequest;
import com.forum.kma.authservice.dto.response.UserResponse;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.repository.UserRepository;
import com.forum.kma.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.forum.kma.authservice.dto.response.PageResponse;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public Mono<PageResponse<UserResponse>> getAllUsers(int page, int size) {
    return userRepository.count()
        .flatMap(total -> userRepository.findAll()
            .skip((long) page * size)
            .take(size)
            .map(this::toResponse)
            .collectList()
            .map(list -> new PageResponse<>(
                list,
                page,
                size,
                total,
                (int) Math.ceil((double) total / size))));
  }

  public Mono<UserResponse> getUserById(String id) {
    return userRepository.findById(id).map(this::toResponse);
  }

    public Mono<UserResponse> getMe() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)

                .map(Authentication::getPrincipal)
                .cast(User.class)

                .flatMap(userPrincipal -> this.getUserById(userPrincipal.getId()))

                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.USER_NOT_EXISTED)));
    }

  public Mono<UserResponse> createUser(UserRequest request) {
    User user = User.builder()
        .username(request.getUsername())
        .password(request.getPassword())
        .email(request.getEmail())
        .roleId(request.getRoleId())
        .userStatus(request.getUserStatus() != null ? User.UserStatus.valueOf(request.getUserStatus()) : User.UserStatus.PENDING)
        .is2FAEnabled(request.getIs2FAEnabled() != null ? request.getIs2FAEnabled() : false)
        .build();
    return userRepository.save(user).map(this::toResponse);
  }

  public Mono<UserResponse> updateUser(String id, UserRequest request) {
    return userRepository.findById(id)
        .flatMap(existing -> {
          existing.setUsername(request.getUsername());
          existing.setPassword(request.getPassword());
          existing.setEmail(request.getEmail());
          existing.setRoleId(request.getRoleId());
          if (request.getUserStatus() != null) existing.setUserStatus(User.UserStatus.valueOf(request.getUserStatus()));
          if (request.getIs2FAEnabled() != null) existing.setIs2FAEnabled(request.getIs2FAEnabled());
          return userRepository.save(existing);
        })
        .map(this::toResponse);
  }

  public Mono<Void> deleteUser(String id) {
    return userRepository.deleteById(id);
  }

  private UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRoleId(),
        user.getUserStatus() != null ? user.getUserStatus().name() : null,
        user.getIs2FAEnabled()
    );
  }
}
