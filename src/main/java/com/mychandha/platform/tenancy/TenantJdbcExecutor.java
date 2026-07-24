package com.mychandha.platform.tenancy;

import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The only supported entry point for organization-scoped SQL. It binds the
 * organization and actor to the current database transaction before executing
 * repository work, allowing PostgreSQL RLS to provide defense in depth.
 */
@Component
public final class TenantJdbcExecutor {

    private final JdbcClient jdbc;
    private final PlatformTransactionManager transactionManager;

    public TenantJdbcExecutor(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactionManager = transactionManager;
    }

    public <T> T read(Function<JdbcClient, T> work) {
        return execute(true, work);
    }

    public <T> T write(Function<JdbcClient, T> work) {
        return execute(false, work);
    }

    private <T> T execute(boolean readOnly, Function<JdbcClient, T> work) {
        OrganizationContext.Scope scope = OrganizationContext.require();
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.setReadOnly(readOnly);
        return transactions.execute(status -> {
            bind(scope);
            return work.apply(jdbc);
        });
    }

    void bind(OrganizationContext.Scope scope) {
        jdbc.sql("SELECT set_config('app.current_organization_id', :organizationId, true)")
                .param("organizationId", scope.organizationId().toString())
                .query(String.class)
                .single();
        jdbc.sql("SELECT set_config('app.current_actor_subject', :actorSubject, true)")
                .param("actorSubject", scope.actor().subject())
                .query(String.class)
                .single();
    }
}
