package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class BetaRegistrationFilterTest {
    @Test
    void registrationRequiresValidInvitationAndGoogleCannotBypassIt() throws Exception {
        check("POST", "/api/v1/athletes/register", null, "long-test-code-for-beta-only", 403, false);
        check("POST", "/api/v1/athletes/register", "wrong", "long-test-code-for-beta-only", 403, false);
        check("POST", "/api/v1/athletes/register", "long-test-code-for-beta-only", "", 503, false);
        check("POST", "/api/v1/athletes/register", "long-test-code-for-beta-only", "long-test-code-for-beta-only", 200, true);
        check("POST", "/api/v1/athletes/auth/google", "long-test-code-for-beta-only", "long-test-code-for-beta-only", 403, false);
        check("POST", "/api/v1/athletes/login", null, "long-test-code-for-beta-only", 200, true);
        check("OPTIONS", "/api/v1/athletes/register", null, "long-test-code-for-beta-only", 200, true);
    }

    private void check(String method, String path, String supplied, String expected, int status, boolean passes) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (supplied != null) request.addHeader("X-Beta-Invite", supplied);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new BetaRegistrationFilter(expected).doFilter(request, response, chain);
        assertEquals(status, response.getStatus());
        assertEquals(passes, chain.getRequest() != null);
    }
}
