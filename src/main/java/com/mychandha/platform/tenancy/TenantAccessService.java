package com.mychandha.platform.tenancy;

import com.mychandha.platform.identity.ExternalIdentity;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TenantAccessService {

    private final JdbcClient jdbc;
    private final PlatformTransactionManager transactionManager;

    public TenantAccessService(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactionManager = transactionManager;
    }

    public boolean hasActiveMembership(UUID organizationId, ExternalIdentity identity) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            bindRequestedTenant(organizationId, identity.subject());
            return jdbc.sql("""
                            SELECT EXISTS (
                                SELECT 1
                                FROM identity.identity_link link
                                JOIN identity.membership membership ON membership.user_id = link.user_id
                                WHERE link.provider = :provider
                                  AND link.external_subject = :subject
                                  AND membership.organization_id = :organizationId
                                  AND membership.status = 'ACTIVE'
                            )
                            """)
                    .param("provider", identity.provider())
                    .param("subject", identity.subject())
                    .param("organizationId", organizationId)
                    .query(Boolean.class)
                    .single();
        }));
    }

    public boolean hasPermission(String permissionCode) {
        OrganizationContext.Scope scope = OrganizationContext.require();
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            bindRequestedTenant(scope.organizationId(), scope.actor().subject());
            return jdbc.sql("""
                            SELECT EXISTS (
                                SELECT 1
                                FROM identity.identity_link link
                                JOIN identity.membership membership ON membership.user_id = link.user_id
                                JOIN identity.membership_role membership_role
                                  ON membership_role.membership_id = membership.id
                                JOIN identity.role_permission role_permission
                                  ON role_permission.role_id = membership_role.role_id
                                JOIN identity.permission permission
                                  ON permission.id = role_permission.permission_id
                                WHERE link.provider = :provider
                                  AND link.external_subject = :subject
                                  AND membership.organization_id = :organizationId
                                  AND membership.status = 'ACTIVE'
                                  AND permission.code = :permission
                            )
                            """)
                    .param("provider", scope.actor().provider())
                    .param("subject", scope.actor().subject())
                    .param("organizationId", scope.organizationId())
                    .param("permission", permissionCode)
                    .query(Boolean.class)
                    .single();
        }));
    }

    private void bindRequestedTenant(UUID organizationId, String actorSubject) {
        jdbc.sql("SELECT set_config('app.current_organization_id', :organizationId, true)")
                .param("organizationId", organizationId.toString())
                .query(String.class)
                .single();
        jdbc.sql("SELECT set_config('app.current_actor_subject', :actorSubject, true)")
                .param("actorSubject", actorSubject)
                .query(String.class)
                .single();
    }
}
