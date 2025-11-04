package com.forum.kma.aclservice.service;

import com.forum.kma.aclservice.model.AclCheckRequest;
import com.forum.kma.aclservice.model.AclCheckResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AclDecisionService {

    public Mono<AclCheckResponse> decide(AclCheckRequest req) {
        // Rule 1: ADMIN luôn được phép
        if (
                "ROLE_ADMIN".equalsIgnoreCase(req.getRoleName()) &&
        ("READ".equalsIgnoreCase(req.getAction())|| "DELETE".equalsIgnoreCase(req.getAction()))
        ) {
            return Mono.just(new AclCheckResponse(true, "Admin override"));
        }

        // Rule 2: Author có toàn quyền với resource của mình
        if (req.getAuthorId() != null && req.getAuthorId().equals(req.getUserId())) {
            return Mono.just(new AclCheckResponse(true, "Author owns this resource"));
        }

        // Rule 3: Người khác chỉ được READ
        if ("READ".equalsIgnoreCase(req.getAction())) {
            return Mono.just(new AclCheckResponse(true, "Public read access"));
        }

        // Nếu không thoả mãn gì hết → từ chối
        return Mono.just(new AclCheckResponse(false, "Permission denied"));
    }
}
