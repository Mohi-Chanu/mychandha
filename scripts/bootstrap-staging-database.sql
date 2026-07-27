\set ON_ERROR_STOP on
\set ECHO none
\set QUIET 1
\set VERBOSITY terse
\set SHOW_CONTEXT never

\getenv api_password MYCHANDHA_STAGING_API_PASSWORD
\getenv dispatcher_password MYCHANDHA_STAGING_DISPATCHER_PASSWORD
\getenv migration_password MYCHANDHA_STAGING_MIGRATION_PASSWORD

SELECT CASE
    WHEN length(:'api_password') BETWEEN 32 AND 256 THEN 1
    ELSE CAST('invalid API role password length' AS INTEGER)
END;
SELECT CASE
    WHEN length(:'dispatcher_password') BETWEEN 32 AND 256 THEN 1
    ELSE CAST('invalid dispatcher role password length' AS INTEGER)
END;
SELECT CASE
    WHEN length(:'migration_password') BETWEEN 32 AND 256 THEN 1
    ELSE CAST('invalid migration role password length' AS INTEGER)
END;

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_api',
    :'api_password'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_staging_api'
)
\gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_dispatcher',
    :'dispatcher_password'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_staging_dispatcher'
)
\gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_migration',
    :'migration_password'
)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_staging_migration'
)
\gexec

SELECT format(
    'ALTER ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_api',
    :'api_password'
)
\gexec
SELECT format(
    'ALTER ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_dispatcher',
    :'dispatcher_password'
)
\gexec
SELECT format(
    'ALTER ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB '
    'NOCREATEROLE NOREPLICATION NOBYPASSRLS',
    'mychandha_staging_migration',
    :'migration_password'
)
\gexec

SELECT format(
    'GRANT CONNECT, CREATE ON DATABASE %I TO %I',
    current_database(),
    'mychandha_staging_migration'
)
\gexec
SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    current_database(),
    'mychandha_staging_api'
)
\gexec
SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    current_database(),
    'mychandha_staging_dispatcher'
)
\gexec

\set api_login_role mychandha_staging_api
\set dispatcher_login_role mychandha_staging_dispatcher
\ir bootstrap-database-roles.sql

SELECT CASE
    WHEN pg_has_role(
        'mychandha_staging_api',
        'mychandha_api',
        'MEMBER'
    ) THEN 1
    ELSE CAST('API group membership missing' AS INTEGER)
END;
SELECT CASE
    WHEN pg_has_role(
        'mychandha_staging_dispatcher',
        'mychandha_dispatcher',
        'MEMBER'
    ) THEN 1
    ELSE CAST('dispatcher group membership missing' AS INTEGER)
END;
SELECT CASE
    WHEN NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname IN (
            'mychandha_staging_api',
            'mychandha_staging_dispatcher',
            'mychandha_staging_migration'
        )
          AND (
              rolsuper
              OR rolcreatedb
              OR rolcreaterole
              OR rolreplication
              OR rolbypassrls
          )
    ) THEN 1
    ELSE CAST('environment login role is privileged' AS INTEGER)
END;

\set api_password ''
\set dispatcher_password ''
\set migration_password ''
\set QUIET 0
\echo 'bootstrap_result=passed roles=3 group_bindings=2'
