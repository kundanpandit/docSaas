package com.docsaas.api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.docsaas.api.exception.ApiException;

public class SecurityUtil {

    public static Long getLoggedInUserId() {
        Long userId = getUserIdOrNull();

        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue");
        }

        return userId;
    }

    /** Returns the authenticated user's id, or null for guests. */
    public static Long getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || auth.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
