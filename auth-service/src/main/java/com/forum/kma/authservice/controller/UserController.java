package com.forum.kma.authservice.controller;

import com.forum.kma.authservice.dto.request.UserRequest;
import com.forum.kma.authservice.dto.response.UserResponse;
import com.forum.kma.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.authservice.dto.response.PageResponse;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping
  public Mono<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return userService.getAllUsers(page, size)
        .map(pageResult -> ApiResponse.success("Fetched all users", pageResult));
  }

  @GetMapping("/{id}")
  public Mono<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
    return userService.getUserById(id)
        .map(data -> ApiResponse.success("Fetched user", data));
  }

  @GetMapping("/me")
  public Mono<ApiResponse<UserResponse>> getAuthenticatedUser() {
    return userService.getMe()
            .map(data -> ApiResponse.success("Get me successfully", data));
  }

  @PostMapping
  public Mono<ApiResponse<UserResponse>> createUser(@RequestBody UserRequest request) {
    return userService.createUser(request)
        .map(data -> ApiResponse.success("Created user", data));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('user:update')")
  public Mono<ApiResponse<UserResponse>> updateUser(@PathVariable String id, @RequestBody UserRequest request) {
    return userService.updateUser(id, request)
        .map(data -> ApiResponse.success("Updated user", data));
  }

  @DeleteMapping("/{id}")
  public Mono<ApiResponse<Void>> deleteUser(@PathVariable String id) {
    return userService.deleteUser(id)
        .thenReturn(ApiResponse.success("Deleted user", null));
  }
}
