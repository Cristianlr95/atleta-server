package com.atleta.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BetaManualWritesFilterTest {
    @Test
    void blocksManualWriteRoutesButAllowsOtherRequests() throws Exception {
        String[][] blockedRoutes = {
                {"POST", "/api/v1/ratings/update"},
                {"POST", "/api/v1/ratings/update-rotative-goalkeeper"},
                {"PUT", "/api/v1/player-profiles/trust-score"},
                {"PUT", "/api/v1/player-profiles/123/positions/4/experience"}
        };

        for (String[] route : blockedRoutes) {
            var request = new MockHttpServletRequest(route[0], route[1]);
            request.setServletPath(route[1]);
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();

            new BetaManualWritesFilter().doFilter(request, response, chain);

            assertEquals(403, response.getStatus(), route[1]);
            assertNull(chain.getRequest(), route[1]);
        }

        var request = new MockHttpServletRequest("GET", "/api/v1/ratings/update");
        request.setServletPath("/api/v1/ratings/update");
        var chain = new MockFilterChain();

        new BetaManualWritesFilter().doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }
}
