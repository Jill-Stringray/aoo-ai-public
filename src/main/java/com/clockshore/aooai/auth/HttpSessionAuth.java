package com.clockshore.aooai.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class HttpSessionAuth {

    private HttpSessionAuth() {
    }

    public static Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object v = session.getAttribute(SessionConstants.USER_ID);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return null;
    }
}
