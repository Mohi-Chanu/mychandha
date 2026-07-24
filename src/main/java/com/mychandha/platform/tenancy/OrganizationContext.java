package com.mychandha.platform.tenancy;

import com.mychandha.platform.identity.ExternalIdentity;
import java.util.Optional;
import java.util.UUID;

public final class OrganizationContext {

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static void set(UUID organizationId, ExternalIdentity actor) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Organization context is already established");
        }
        CURRENT.set(new Scope(organizationId, actor));
    }

    public static Scope require() {
        return Optional.ofNullable(CURRENT.get())
                .orElseThrow(() -> new OrganizationContextMissingException(
                        "Supply X-Organization-Id for this operation."));
    }

    public static Optional<Scope> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Scope(UUID organizationId, ExternalIdentity actor) {
    }
}
