package com.docsaas.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getLoggedInUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        return Long.parseLong(auth.getPrincipal().toString());
    }
}
