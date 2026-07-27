package com.mychandha.platform.security.ratelimit;

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
public final class ClientAddressRateLimitFilter extends OncePerRequestFilter {

    private static final String PROCESS_KEY = "single-api-instance";
    private final TrustedProxyClientAddressResolver addressResolver;
    private final RateLimitEndpointClassifier endpointClassifier;
    private final RateLimitService rateLimits;
    private final RateLimitProblemWriter problemWriter;

    ClientAddressRateLimitFilter(
            TrustedProxyClientAddressResolver addressResolver,
            RateLimitEndpointClassifier endpointClassifier,
            RateLimitService rateLimits,
            RateLimitProblemWriter problemWriter) {
        this.addressResolver = addressResolver;
        this.endpointClassifier = endpointClassifier;
        this.rateLimits = rateLimits;
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitEndpointClass endpointClass = endpointClassifier.classify(request);
        RateLimitDecision process = rateLimits.check(
                RateLimitScope.PROCESS, PROCESS_KEY, endpointClass);
        if (!process.allowed()) {
            problemWriter.write(response, process);
            return;
        }
        RateLimitDecision address = rateLimits.check(
                RateLimitScope.CLIENT_ADDRESS,
                addressResolver.resolve(request),
                endpointClass);
        if (!address.allowed()) {
            problemWriter.write(response, address);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
