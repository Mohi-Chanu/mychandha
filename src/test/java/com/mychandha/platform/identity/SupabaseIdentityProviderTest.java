package com.mychandha.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SupabaseIdentityProviderTest {

    private final SupabaseIdentityProvider provider = new SupabaseIdentityProvider();

    @Test
    void mapsStableIdentityClaims() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "25ed12c9-fc9e-4d84-87f2-f430b17c8d6d",
                        "email", "donor@example.com",
                        "phone", "+919999999999",
                        "aud", List.of("authenticated")));

        ExternalIdentity identity = provider.resolve(new JwtAuthenticationToken(jwt));

        assertThat(identity.provider()).isEqualTo("supabase");
        assertThat(identity.subject()).isEqualTo("25ed12c9-fc9e-4d84-87f2-f430b17c8d6d");
        assertThat(identity.email()).isEqualTo("donor@example.com");
        assertThat(identity.phone()).isEqualTo("+919999999999");
    }
}
