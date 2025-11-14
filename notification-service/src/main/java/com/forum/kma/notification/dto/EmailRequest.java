package com.forum.kma.notification.dto;

import java.util.List;

public record EmailRequest(
        List<Recipient> to,
        String subject,
        String htmlContent
) {}
