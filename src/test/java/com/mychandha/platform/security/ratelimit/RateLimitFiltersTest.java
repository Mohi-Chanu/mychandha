package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.identity.ExternalIdentity;
import com.mychandha.platform.tenancy.OrganizationContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFiltersTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final RateLimitProperties properties = RateLimitTestSupport.properties(1);
    private final RateLimitService service = new RateLimitService(
            properties,
            new RateLimitKeyHasher(),
            new RateLimitMetrics(meters),
            new MutableTimeMeter());
    private final RateLimitEndpointClassifier classifier =
            new RateLimitEndpointClassifier();
    private final RateLimitProblemWriter writer =
            new RateLimitProblemWriter(new ObjectMapper());

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        OrganizationContext.clear();
        meters.close();
    }

    @Test
    void clientFilterRejectsRepeatedAddressWithProblemDetails() throws Exception {
        ClientAddressRateLimitFilter filter = new ClientAddressRateLimitFilter(
                new TrustedProxyClientAddressResolver(properties),
                classifier,
                service,
                writer);

        MockHttpServletResponse first = execute(filter, request("203.0.113.20"));
        MockHttpServletResponse second = execute(filter, request("203.0.113.20"));

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isEqualTo("60");
        assertThat(second.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void subjectFilterUsesAuthenticatedPrincipalOnly() throws Exception {
        SubjectRateLimitFilter filter =
                new SubjectRateLimitFilter(classifier, service, writer);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("subject-1", "unused", "ROLE_USER"));

        MockHttpServletResponse first = execute(filter, request("203.0.113.21"));
        MockHttpServletResponse second = execute(filter, request("203.0.113.22"));

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
    }

    @Test
    void organizationFilterUsesOnlyAnAuthorizedContext() throws Exception {
        OrganizationRateLimitFilter filter =
                new OrganizationRateLimitFilter(classifier, service, writer);
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OrganizationContext.set(
                organizationId,
                new ExternalIdentity("supabase", "subject-1", null, null, Map.of()));

        MockHttpServletResponse first = execute(filter, request("203.0.113.23"));
        MockHttpServletResponse second = execute(filter, request("203.0.113.24"));

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/platform/me");
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private MockHttpServletResponse execute(
            org.springframework.web.filter.OncePerRequestFilter filter,
            MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
