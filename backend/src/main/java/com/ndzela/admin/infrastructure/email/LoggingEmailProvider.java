package com.ndzela.admin.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log")
public class LoggingEmailProvider implements EmailProvider {

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.warn("[DEV MAIL - not sent] to={} subject='{}' template={} variables={}", to, subject, templateName, variables);
    }
}
