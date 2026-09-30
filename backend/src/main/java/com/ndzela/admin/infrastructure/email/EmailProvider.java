package com.ndzela.admin.infrastructure.email;

import java.util.Map;

public interface EmailProvider {
    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);
}
