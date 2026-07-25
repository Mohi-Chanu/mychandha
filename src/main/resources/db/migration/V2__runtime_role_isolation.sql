ALTER TABLE platform.outbox_event
    ADD COLUMN trace_parent TEXT;

ALTER TABLE platform.outbox_event
    ADD CONSTRAINT outbox_event_trace_parent_format
    CHECK (
        trace_parent IS NULL
        OR trace_parent ~ '^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$'
    );

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_api') THEN
        RAISE EXCEPTION
            'required group role mychandha_api is missing; run scripts/bootstrap-database-roles.sql';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_dispatcher') THEN
        RAISE EXCEPTION
            'required group role mychandha_dispatcher is missing; run scripts/bootstrap-database-roles.sql';
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION platform.claim_outbox_events(
    p_worker_id TEXT,
    p_batch_size INTEGER,
    p_lock_timeout_seconds BIGINT
)
RETURNS TABLE (
    id UUID,
    organization_id UUID,
    aggregate_type TEXT,
    aggregate_id TEXT,
    event_type TEXT,
    schema_version INTEGER,
    payload TEXT,
    correlation_id TEXT,
    trace_parent TEXT,
    attempts INTEGER,
    reclaimed BOOLEAN
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF p_worker_id IS NULL
       OR p_worker_id !~ '^[A-Za-z0-9._:-]{1,100}$' THEN
        RAISE EXCEPTION 'invalid dispatcher identity'
            USING ERRCODE = '22023';
    END IF;
    IF p_batch_size IS NULL OR p_batch_size NOT BETWEEN 1 AND 500 THEN
        RAISE EXCEPTION 'batch size must be between 1 and 500'
            USING ERRCODE = '22023';
    END IF;
    IF p_lock_timeout_seconds IS NULL
       OR p_lock_timeout_seconds NOT BETWEEN 1 AND 86400 THEN
        RAISE EXCEPTION 'lock timeout must be between 1 and 86400 seconds'
            USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    WITH candidates AS (
        SELECT candidate.id,
               candidate.status = 'PROCESSING' AS reclaimed
        FROM platform.outbox_event AS candidate
        WHERE (candidate.status = 'PENDING'
               AND candidate.available_at <= pg_catalog.now())
           OR (candidate.status = 'PROCESSING'
               AND candidate.locked_at
                   < pg_catalog.now()
                     - (p_lock_timeout_seconds * interval '1 second'))
        ORDER BY candidate.created_at
        FOR UPDATE SKIP LOCKED
        LIMIT p_batch_size
    ),
    claimed AS (
        UPDATE platform.outbox_event AS event
        SET status = 'PROCESSING',
            attempts = event.attempts + 1,
            locked_at = pg_catalog.now(),
            locked_by = p_worker_id
        FROM candidates
        WHERE event.id = candidates.id
        RETURNING event.id,
                  event.organization_id,
                  event.aggregate_type,
                  event.aggregate_id,
                  event.event_type,
                  event.schema_version,
                  event.payload::TEXT,
                  event.correlation_id,
                  event.trace_parent,
                  event.attempts,
                  candidates.reclaimed
    )
    SELECT claimed.id,
           claimed.organization_id,
           claimed.aggregate_type,
           claimed.aggregate_id,
           claimed.event_type,
           claimed.schema_version,
           claimed.payload,
           claimed.correlation_id,
           claimed.trace_parent,
           claimed.attempts,
           claimed.reclaimed
    FROM claimed;
END
$$;

CREATE OR REPLACE FUNCTION platform.mark_outbox_published(
    p_event_id UUID,
    p_worker_id TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    affected INTEGER;
BEGIN
    IF p_event_id IS NULL THEN
        RAISE EXCEPTION 'event id is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_worker_id IS NULL
       OR p_worker_id !~ '^[A-Za-z0-9._:-]{1,100}$' THEN
        RAISE EXCEPTION 'invalid dispatcher identity'
            USING ERRCODE = '22023';
    END IF;

    UPDATE platform.outbox_event
    SET status = 'PUBLISHED',
        published_at = pg_catalog.now(),
        locked_at = NULL,
        locked_by = NULL,
        last_error_code = NULL
    WHERE id = p_event_id
      AND status = 'PROCESSING'
      AND locked_by = p_worker_id;

    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected = 1;
END
$$;

CREATE OR REPLACE FUNCTION platform.reschedule_outbox_event(
    p_event_id UUID,
    p_worker_id TEXT,
    p_backoff_seconds BIGINT,
    p_error_code TEXT,
    p_dead_letter BOOLEAN
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    affected INTEGER;
BEGIN
    IF p_event_id IS NULL THEN
        RAISE EXCEPTION 'event id is required'
            USING ERRCODE = '22023';
    END IF;
    IF p_worker_id IS NULL
       OR p_worker_id !~ '^[A-Za-z0-9._:-]{1,100}$' THEN
        RAISE EXCEPTION 'invalid dispatcher identity'
            USING ERRCODE = '22023';
    END IF;
    IF p_backoff_seconds IS NULL OR p_backoff_seconds NOT BETWEEN 1 AND 3600 THEN
        RAISE EXCEPTION 'backoff must be between 1 and 3600 seconds'
            USING ERRCODE = '22023';
    END IF;
    IF p_error_code IS NULL
       OR p_error_code !~ '^[A-Za-z0-9._:-]{1,100}$' THEN
        RAISE EXCEPTION 'invalid error code'
            USING ERRCODE = '22023';
    END IF;
    IF p_dead_letter IS NULL THEN
        RAISE EXCEPTION 'dead-letter decision is required'
            USING ERRCODE = '22023';
    END IF;

    UPDATE platform.outbox_event
    SET status = CASE WHEN p_dead_letter THEN 'DEAD_LETTER' ELSE 'PENDING' END,
        available_at = pg_catalog.now()
            + (p_backoff_seconds * interval '1 second'),
        locked_at = NULL,
        locked_by = NULL,
        last_error_code = p_error_code
    WHERE id = p_event_id
      AND status = 'PROCESSING'
      AND locked_by = p_worker_id;

    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected = 1;
END
$$;

CREATE OR REPLACE FUNCTION platform.outbox_backlog(
    p_lock_timeout_seconds BIGINT
)
RETURNS TABLE (
    pending BIGINT,
    oldest TIMESTAMPTZ,
    retry_attempts BIGINT,
    dead_letter BIGINT,
    stale_processing BIGINT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, pg_temp
AS $$
BEGIN
    IF p_lock_timeout_seconds IS NULL
       OR p_lock_timeout_seconds NOT BETWEEN 1 AND 86400 THEN
        RAISE EXCEPTION 'lock timeout must be between 1 and 86400 seconds'
            USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT count(*) FILTER (
               WHERE event.status IN ('PENDING', 'PROCESSING'))::BIGINT,
           min(event.created_at) FILTER (
               WHERE event.status IN ('PENDING', 'PROCESSING')),
           coalesce(sum(event.attempts) FILTER (
               WHERE event.status IN ('PENDING', 'PROCESSING')), 0)::BIGINT,
           count(*) FILTER (
               WHERE event.status = 'DEAD_LETTER')::BIGINT,
           count(*) FILTER (
               WHERE event.status = 'PROCESSING'
                 AND event.locked_at
                     < pg_catalog.now()
                       - (p_lock_timeout_seconds * interval '1 second'))::BIGINT
    FROM platform.outbox_event AS event;
END
$$;

REVOKE CREATE ON SCHEMA organization, identity, audit, platform FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA organization, identity, audit, platform
    FROM mychandha_api, mychandha_dispatcher;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA organization, identity, audit, platform
    FROM mychandha_api, mychandha_dispatcher;
REVOKE ALL ON FUNCTION platform.claim_outbox_events(TEXT, INTEGER, BIGINT)
    FROM PUBLIC, mychandha_api, mychandha_dispatcher;
REVOKE ALL ON FUNCTION platform.mark_outbox_published(UUID, TEXT)
    FROM PUBLIC, mychandha_api, mychandha_dispatcher;
REVOKE ALL ON FUNCTION platform.reschedule_outbox_event(
    UUID, TEXT, BIGINT, TEXT, BOOLEAN)
    FROM PUBLIC, mychandha_api, mychandha_dispatcher;
REVOKE ALL ON FUNCTION platform.outbox_backlog(BIGINT)
    FROM PUBLIC, mychandha_api, mychandha_dispatcher;
REVOKE EXECUTE ON FUNCTION platform.reject_immutable_change() FROM PUBLIC;

GRANT USAGE ON SCHEMA organization, identity, audit, platform
    TO mychandha_api;
GRANT SELECT, INSERT, UPDATE ON organization.organization
    TO mychandha_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON identity.user_profile
    TO mychandha_api;
GRANT SELECT, INSERT, UPDATE ON identity.identity_link
    TO mychandha_api;
GRANT SELECT, INSERT, UPDATE ON identity.membership, identity.role
    TO mychandha_api;
GRANT SELECT, INSERT, DELETE ON identity.membership_role, identity.role_permission
    TO mychandha_api;
GRANT SELECT ON identity.permission
    TO mychandha_api;
GRANT SELECT, INSERT ON audit.audit_event
    TO mychandha_api;
GRANT INSERT ON platform.outbox_event
    TO mychandha_api;
GRANT SELECT, INSERT, UPDATE ON platform.inbox_event, platform.idempotency_record
    TO mychandha_api;

GRANT USAGE ON SCHEMA platform
    TO mychandha_dispatcher;
GRANT EXECUTE ON FUNCTION platform.claim_outbox_events(TEXT, INTEGER, BIGINT)
    TO mychandha_dispatcher;
GRANT EXECUTE ON FUNCTION platform.mark_outbox_published(UUID, TEXT)
    TO mychandha_dispatcher;
GRANT EXECUTE ON FUNCTION platform.reschedule_outbox_event(
    UUID, TEXT, BIGINT, TEXT, BOOLEAN)
    TO mychandha_dispatcher;
GRANT EXECUTE ON FUNCTION platform.outbox_backlog(BIGINT)
    TO mychandha_dispatcher;
