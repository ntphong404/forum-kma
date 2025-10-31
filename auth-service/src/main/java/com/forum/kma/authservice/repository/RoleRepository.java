package com.forum.kma.authservice.repository;

import com.forum.kma.authservice.model.Role;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import org.springframework.data.mongodb.repository.Query;
import reactor.core.publisher.Mono;

public interface RoleRepository extends ReactiveMongoRepository<Role, String> {
  @Query("{ 'name': ?0 }")
  Mono<Role> findByName(String name);
}
