package com.forum.kma.authservice.service;

import com.forum.kma.authservice.dto.RoleRequest;
import com.forum.kma.authservice.model.Role;
import com.forum.kma.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoleService {
  private final RoleRepository roleRepository;

  public Mono<Role> createRole(RoleRequest request) {
    Role role = Role.builder()
        .name(request.getName())
        .permissions(request.getPermissions()) // Set<String> tên permission
        .build();
    return roleRepository.save(role);
  }

  public Mono<Role> updateRole(String id, RoleRequest request) {
    return roleRepository.findById(id)
        .flatMap(role -> {
          role.setName(request.getName());
          role.setPermissions(request.getPermissions()); // Set<String> tên permission
          return roleRepository.save(role);
        });
  }

  public Mono<Void> deleteRole(String id) {
    return roleRepository.deleteById(id);
  }

  public Mono<Role> getRoleById(String id) {
    return roleRepository.findById(id);
  }

  public Flux<Role> getAllRoles(int page, int size) {
    return roleRepository.findAll()
        .skip((long) page * size)
        .take(size);
  }
}
