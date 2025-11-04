package com.forum.kma.aclservice.controller;

import com.forum.kma.aclservice.model.AclCheckRequest;
import com.forum.kma.aclservice.model.AclCheckResponse;
import com.forum.kma.aclservice.service.AclDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/acl")
@RequiredArgsConstructor
public class AclController {

    private final AclDecisionService aclDecisionService;

    @PostMapping("/check")
    public Mono<AclCheckResponse> checkPermission(@RequestBody AclCheckRequest request) {
        return aclDecisionService.decide(request);
    }
}
