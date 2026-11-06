package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import static org.junit.jupiter.api.Assertions.*;

class BetaUploadsFilterTest {
    @Test
    void blocksBothUploadRoutesButAllowsNormalRequests() throws Exception {
        for (String path : new String[]{"/api/v1/teams/logo", "/api/v1/player-profiles/123/picture"}) {
            var request = new MockHttpServletRequest("POST", path);
            request.setServletPath(path);
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            new BetaUploadsFilter().doFilter(request, response, chain);
            assertEquals(503, response.getStatus());
            assertNull(chain.getRequest());
        }
        var request = new MockHttpServletRequest("POST", "/api/v1/teams");
        request.setServletPath("/api/v1/teams");
        var chain = new MockFilterChain();
        new BetaUploadsFilter().doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
    }
}
