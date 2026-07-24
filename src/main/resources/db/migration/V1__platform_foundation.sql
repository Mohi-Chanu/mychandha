CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE organization.organization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name TEXT NOT NULL,
    display_name TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('DRAFT', 'VERIFICATION_PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE identity.user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_masked TEXT,
    phone_masked TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE identity.identity_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES identity.user_profile(id),
    provider TEXT NOT NULL,
    external_subject TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, external_subject)
);
CREATE INDEX identity_link_user_idx ON identity.identity_link(user_id);

CREATE TABLE identity.membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    user_id UUID NOT NULL REFERENCES identity.user_profile(id),
    status TEXT NOT NULL CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (organization_id, user_id),
    UNIQUE (organization_id, id)
);
CREATE INDEX membership_user_idx ON identity.membership(user_id, organization_id);

CREATE TABLE identity.role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (organization_id, code),
    UNIQUE (organization_id, id)
);

CREATE TABLE identity.permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL
);

CREATE TABLE identity.membership_role (
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    membership_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES identity.user_profile(id),
    PRIMARY KEY (membership_id, role_id),
    FOREIGN KEY (organization_id, membership_id)
        REFERENCES identity.membership(organization_id, id),
    FOREIGN KEY (organization_id, role_id)
        REFERENCES identity.role(organization_id, id)
);

CREATE TABLE identity.role_permission (
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL REFERENCES identity.permission(id),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (organization_id, role_id)
        REFERENCES identity.role(organization_id, id)
);

INSERT INTO identity.permission (code, description)
VALUES ('platform.access', 'Access the authenticated organization workspace')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE audit.audit_event (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    sequence_number BIGINT NOT NULL CHECK (sequence_number > 0),
    event_type TEXT NOT NULL,
    actor_provider TEXT NOT NULL,
    actor_subject TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT,
    correlation_id TEXT,
    event_data JSONB NOT NULL,
    event_data_canonical TEXT NOT NULL,
    previous_hash TEXT,
    event_hash TEXT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX audit_event_org_sequence_uidx
    ON audit.audit_event(organization_id, sequence_number);
CREATE INDEX audit_event_org_time_idx
    ON audit.audit_event(organization_id, recorded_at DESC);

CREATE TABLE platform.outbox_event (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    aggregate_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    schema_version INTEGER NOT NULL CHECK (schema_version > 0),
    payload JSONB NOT NULL,
    correlation_id TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'DEAD_LETTER')),
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_at TIMESTAMPTZ,
    locked_by TEXT,
    published_at TIMESTAMPTZ,
    last_error_code TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX outbox_dispatch_idx ON platform.outbox_event(status, available_at, created_at);

CREATE TABLE platform.inbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    source TEXT NOT NULL,
    external_event_id TEXT NOT NULL,
    payload_hash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'PROCESSED', 'REJECTED')),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    UNIQUE (organization_id, source, external_event_id)
);

CREATE TABLE platform.idempotency_record (
    organization_id UUID NOT NULL REFERENCES organization.organization(id),
    actor_subject TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    response_status INTEGER,
    response_body JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (organization_id, actor_subject, idempotency_key)
);

CREATE OR REPLACE FUNCTION platform.reject_immutable_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'immutable records cannot be updated or deleted'
        USING ERRCODE = '55000';
END
$$;

CREATE TRIGGER audit_event_immutable
BEFORE UPDATE OR DELETE ON audit.audit_event
FOR EACH ROW EXECUTE FUNCTION platform.reject_immutable_change();

CREATE TRIGGER inbox_event_no_delete
BEFORE DELETE ON platform.inbox_event
FOR EACH ROW EXECUTE FUNCTION platform.reject_immutable_change();

ALTER TABLE organization.organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization.organization FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_isolation ON organization.organization
    USING (id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE identity.membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.membership FORCE ROW LEVEL SECURITY;
CREATE POLICY membership_isolation ON identity.membership
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE identity.role ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.role FORCE ROW LEVEL SECURITY;
CREATE POLICY role_isolation ON identity.role
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE identity.membership_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.membership_role FORCE ROW LEVEL SECURITY;
CREATE POLICY membership_role_isolation ON identity.membership_role
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE identity.role_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.role_permission FORCE ROW LEVEL SECURITY;
CREATE POLICY role_permission_isolation ON identity.role_permission
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE audit.audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit.audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_event_isolation ON audit.audit_event
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

-- Dispatcher tables are RLS-protected for ordinary queries. The migration owner
-- is intentionally not forced through RLS so the durable worker can claim work
-- across organizations. Production must use separate migration/worker and API
-- database roles before organization product data is enabled.
ALTER TABLE platform.outbox_event ENABLE ROW LEVEL SECURITY;
CREATE POLICY outbox_event_isolation ON platform.outbox_event
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE platform.inbox_event ENABLE ROW LEVEL SECURITY;
CREATE POLICY inbox_event_isolation ON platform.inbox_event
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);

ALTER TABLE platform.idempotency_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform.idempotency_record FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotency_record_isolation ON platform.idempotency_record
    USING (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid)
    WITH CHECK (organization_id = nullif(current_setting('app.current_organization_id', true), '')::uuid);
