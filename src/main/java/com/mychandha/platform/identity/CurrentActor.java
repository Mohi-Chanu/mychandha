package com.mychandha.platform.identity;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public final class CurrentActor {

    private final IdentityProviderRegistry providers;

    public CurrentActor(IdentityProviderRegistry providers) {
        this.providers = providers;
    }

    public ExternalIdentity require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated actor is required");
        }
        return providers.activeProvider().resolve(authentication);
    }
}
