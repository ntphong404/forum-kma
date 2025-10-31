//package com.forum.kma.authservice.controller;
//
//import com.forum.kma.authservice.model.Role;
//import com.forum.kma.authservice.repository.RoleRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//import reactor.core.publisher.Mono;
//
//import java.util.Collections;
//import java.util.Set;
//
//@Slf4j
//@RestController
//@RequestMapping("/internal")
//@RequiredArgsConstructor
//public class InternalController {
//
//    private final RoleRepository roleRepository;
//
//    @Value("${internal.gateway.secret-key}")
//    private String gatewaySecret;
//
//    // Endpoint Introspection chỉ dùng cho API Gateway
//    @GetMapping("/permissions/{roleId}")
//    public Mono<Set<String>> getPermissionsByRoleId(
//            @PathVariable String roleId,
//            // Đọc Header Secret Key từ Gateway
//            @RequestHeader(value = "X-Internal-Secret") String providedSecret) {
//log.info("getHeader {}", providedSecret);
//        // 1. KIỂM TRA BÍ MẬT NỘI BỘ
//        if (!providedSecret.equals(gatewaySecret)) {
//            // Trả về 403 Forbidden nếu Secret Key sai
//            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret key."));
//        }
//
//        // 2. TRUY VẤN DB CỦA CHÍNH AUTH SERVICE
//        return roleRepository.findById(roleId)
//                // Giả sử Role entity có phương thức getPermissions() trả về Set<String>
//                .map(Role::getPermissions)
//                .switchIfEmpty(Mono.just(Collections.emptySet()));
//    }
//}