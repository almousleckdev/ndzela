package com.ndzela.admin.auth.application.model;

public record TokenRotation(String email, String newToken) {}
