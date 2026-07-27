package com.mychandha.platform.security.ratelimit;

import com.mychandha.platform.tenancy.OrganizationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"api", "local", "test"})
public final class OrganizationRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitEndpointClassifier endpointClassifier;
    private final RateLimitService rateLimits;
    private final RateLimitProblemWriter problemWriter;

    OrganizationRateLimitFilter(
            RateLimitEndpointClassifier endpointClassifier,
            RateLimitService rateLimits,
            RateLimitProblemWriter problemWriter) {
        this.endpointClassifier = endpointClassifier;
        this.rateLimits = rateLimits;
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var organization = OrganizationContext.current();
        if (organization.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimitEndpointClass endpointClass = endpointClassifier.classify(request);
        RateLimitDecision decision = rateLimits.check(
                RateLimitScope.ORGANIZATION,
                organization.get().organizationId().toString(),
                endpointClass);
        if (!decision.allowed()) {
            problemWriter.write(response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
