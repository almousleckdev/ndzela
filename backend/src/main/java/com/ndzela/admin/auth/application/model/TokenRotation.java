package com.ndzela.admin.auth.application.model;

public record TokenRotation(String subject, String sessionId, String newToken) {}
