package com.mychandha.platform.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"api", "local", "test"})
public final class SubjectRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitEndpointClassifier endpointClassifier;
    private final RateLimitService rateLimits;
    private final RateLimitProblemWriter problemWriter;

    SubjectRateLimitFilter(
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitEndpointClass endpointClass = endpointClassifier.classify(request);
        RateLimitScope scope = endpointClass == RateLimitEndpointClass.METRICS
                ? RateLimitScope.METRICS
                : RateLimitScope.SUBJECT;
        RateLimitDecision decision =
                rateLimits.check(scope, authentication.getName(), endpointClass);
        if (!decision.allowed()) {
            problemWriter.write(response, decision);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
