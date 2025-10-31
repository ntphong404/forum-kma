package com.forum.kma.authservice.controller;

import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.authservice.dto.RoleRequest;
import com.forum.kma.authservice.model.Role;
import com.forum.kma.authservice.service.RoleService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {
  private final RoleService roleService;

  @PostMapping
  @PreAuthorize("hasAuthority('role:manage')")
  public Mono<ApiResponse<Role>> createRole(@RequestBody RoleRequest request) {
    return roleService.createRole(request)
        .map(data -> ApiResponse.success("Created role", data));
  }

  @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
  public Mono<ApiResponse<Role>> updateRole(@PathVariable String id, @RequestBody RoleRequest request) {
    return roleService.updateRole(id, request)
        .map(data -> ApiResponse.success("Updated role", data));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public Mono<ApiResponse<Void>> deleteRole(@PathVariable String id) {
    return roleService.deleteRole(id)
        .thenReturn(ApiResponse.success("Deleted role", null));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public Mono<ApiResponse<Role>> getRoleById(@PathVariable String id) {
    return roleService.getRoleById(id)
        .map(data -> ApiResponse.success("Fetched role", data));
  }

  @GetMapping
    @PreAuthorize("hasAuthority('role:manage')")
  public Mono<ApiResponse<List<Role>>> getAllRoles(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return roleService.getAllRoles(page, size)
        .collectList()
        .map(list -> ApiResponse.success("Fetched all roles", list));
  }
}
