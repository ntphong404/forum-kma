package com.forum.kma.notification.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class ThymeleafTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public ThymeleafTemplateRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Render a Thymeleaf template to HTML string off the event-loop.
     */
    public Mono<String> render(String templateName, Map<String, Object> model) {
        return Mono.fromCallable(() -> {
            Context ctx = new Context();
            if (model != null) ctx.setVariables(model);
            return templateEngine.process(templateName, ctx);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
