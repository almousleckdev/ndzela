package com.ndzela.admin.infrastructure.zella;

// A driver we ask about: our own id (echoed back), lowercase e-mail and E.164 phone
public record ZellaDriver(String ref, String email, String phoneE164) {}
