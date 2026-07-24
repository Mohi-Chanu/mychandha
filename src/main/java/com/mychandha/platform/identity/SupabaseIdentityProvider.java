package com.mychandha.platform.identity;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class SupabaseIdentityProvider implements IdentityProvider {

    @Override
    public String providerName() {
        return "supabase";
    }

    @Override
    public ExternalIdentity resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("Expected JWT authentication");
        }
        var jwt = jwtAuthentication.getToken();
        return new ExternalIdentity(
                providerName(),
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("phone"),
                Map.copyOf(jwt.getClaims()));
    }
}
