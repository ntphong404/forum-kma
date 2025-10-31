package com.forum.kma.authservice.service;

import com.forum.kma.authservice.constant.AuthErrorCode;
import com.forum.kma.authservice.dto.UserRequest;
import com.forum.kma.authservice.dto.UserResponse;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.repository.UserRepository;
import com.forum.kma.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.forum.kma.authservice.dto.PageResponse;

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

                .flatMap(userPrincipal -> {
                    return this.getUserById(userPrincipal.getId());
                })

                .switchIfEmpty(Mono.error(new AppException(AuthErrorCode.USER_NOT_EXISTED)));
    }

  public Mono<UserResponse> createUser(UserRequest request) {
    User user = User.builder()
        .username(request.getUsername())
        .password(request.getPassword())
        .email(request.getEmail())
        .roleId(request.getRoleId())
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
          return userRepository.save(existing);
        })
        .map(this::toResponse);
  }

  public Mono<Void> deleteUser(String id) {
    return userRepository.deleteById(id);
  }

  private UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoleId());
  }
}
