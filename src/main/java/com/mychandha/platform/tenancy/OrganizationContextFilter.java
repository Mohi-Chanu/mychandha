package com.mychandha.platform.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.identity.CurrentActor;
import com.mychandha.platform.identity.ExternalIdentity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class OrganizationContextFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Organization-Id";

    private final CurrentActor currentActor;
    private final TenantAccessService tenantAccess;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Dependencies are container-managed stateless collaborators.")
    public OrganizationContextFilter(
            CurrentActor currentActor,
            TenantAccessService tenantAccess,
            ObjectMapper objectMapper) {
        this.currentActor = currentActor;
        this.tenantAccess = tenantAccess;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String rawOrganizationId = request.getHeader(HEADER);
        if (rawOrganizationId == null || rawOrganizationId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }
            ExternalIdentity identity = currentActor.require(authentication);
            UUID organizationId = UUID.fromString(rawOrganizationId);
            if (!tenantAccess.hasActiveMembership(organizationId, identity)) {
                writeProblem(response, 403, "ORGANIZATION_ACCESS_DENIED",
                        "You do not have access to this organization.");
                return;
            }
            OrganizationContext.set(organizationId, identity);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            writeProblem(response, 400, "INVALID_ORGANIZATION_CONTEXT",
                    "X-Organization-Id must be a UUID.");
        } finally {
            OrganizationContext.clear();
        }
    }

    private void writeProblem(
            HttpServletResponse response,
            int status,
            String code,
            String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setTitle(status == 403 ? "Access denied" : "Invalid organization context");
        problem.setType(URI.create("https://api.mychandha.in/problems/"
                + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", MDC.get("correlationId"));
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
