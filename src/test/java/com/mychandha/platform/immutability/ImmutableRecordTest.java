package com.mychandha.platform.immutability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mychandha.platform.audit.AuditEvent;
import com.mychandha.platform.identity.ExternalIdentity;
import com.mychandha.platform.tenancy.OrganizationAccessController.AccessResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmutableRecordTest {

    @Test
    void auditEventDefensivelyCopiesData() {
        Map<String, Object> source = new HashMap<>();
        source.put("status", "requested");
        AuditEvent event = new AuditEvent("refund.requested", "refund", "1", source);

        source.put("status", "approved");

        assertThat(event.data()).containsEntry("status", "requested");
        assertThatThrownBy(() -> event.data().put("status", "rejected"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void externalIdentityDefensivelyCopiesClaims() {
        Map<String, Object> source = new HashMap<>();
        source.put("role", "member");
        ExternalIdentity identity =
                new ExternalIdentity("supabase", "subject", null, null, source);

        source.put("role", "owner");

        assertThat(identity.claims()).containsEntry("role", "member");
        assertThatThrownBy(() -> identity.claims().put("role", "treasurer"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void accessResponseDefensivelyCopiesPermissions() {
        Set<String> source = new HashSet<>();
        source.add("platform.access");
        AccessResponse response = new AccessResponse(UUID.randomUUID(), source);

        source.add("platform.admin");

        assertThat(response.confirmedPermissions()).containsExactly("platform.access");
        assertThatThrownBy(() -> response.confirmedPermissions().add("platform.admin"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
