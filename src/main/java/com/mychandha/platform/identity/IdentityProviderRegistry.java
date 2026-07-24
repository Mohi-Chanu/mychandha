package com.mychandha.platform.identity;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public final class IdentityProviderRegistry {

    private final IdentityProvider activeProvider;

    public IdentityProviderRegistry(
            java.util.List<IdentityProvider> providers,
            IdentityProviderProperties properties) {
        Map<String, IdentityProvider> byName = providers.stream()
                .collect(Collectors.toUnmodifiableMap(IdentityProvider::providerName, Function.identity()));
        this.activeProvider = java.util.Optional.ofNullable(byName.get(properties.provider()))
                .orElseThrow(() -> new IllegalStateException(
                        "No identity provider adapter registered for " + properties.provider()));
    }

    public IdentityProvider activeProvider() {
        return activeProvider;
    }
}
