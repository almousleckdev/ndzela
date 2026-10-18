package com.ndzela.admin.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// The caller's address for the current request, when there is one (a background job has none).
public final class ClientAddress {
    private ClientAddress() {}

    public static String current() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? ((HttpServletRequest) attributes.getRequest()).getRemoteAddr() : null;
    }
}
