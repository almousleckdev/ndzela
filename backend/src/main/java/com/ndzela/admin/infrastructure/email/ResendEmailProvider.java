package com.ndzela.admin.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

/** Sends email through the Resend HTTPS API (port 443), for networks that block outbound SMTP. */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailProvider implements EmailProvider {
    private final TemplateEngine templateEngine;
    private final RestClient client;
    private final String from;

    public ResendEmailProvider(TemplateEngine templateEngine,
                               @Value("${app.mail.resend.api-key}") String apiKey,
                               @Value("${app.mail.resend.from}") String from) {
        this.templateEngine = templateEngine;
        this.from = from;
        this.client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String html = templateEngine.process(templateName, context);
        try {
            client.post().uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", from, "to", List.of(to), "subject", subject, "html", html))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent '{}' email to {} via Resend", templateName, to);
        } catch (RestClientException e) {
            log.error("Failed to send '{}' email to {} via Resend: {}", templateName, to, e.getMessage());
        }
    }
}
