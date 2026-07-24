package com.mychandha.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mychandha.platform.identity.ExternalIdentity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrganizationContextTest {

    @AfterEach
    void clear() {
        OrganizationContext.clear();
    }

    @Test
    void contextMustBeExplicitAndCannotBeReplaced() {
        UUID organizationId = UUID.randomUUID();
        ExternalIdentity actor = new ExternalIdentity("supabase", "subject", null, null, Map.of());

        assertThatThrownBy(OrganizationContext::require)
                .isInstanceOf(OrganizationContextMissingException.class);

        OrganizationContext.set(organizationId, actor);

        assertThat(OrganizationContext.require().organizationId()).isEqualTo(organizationId);
        assertThatThrownBy(() -> OrganizationContext.set(UUID.randomUUID(), actor))
                .isInstanceOf(IllegalStateException.class);
    }
}
