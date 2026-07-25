\set ON_ERROR_STOP on

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_api') THEN
        CREATE ROLE mychandha_api
            NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'mychandha_dispatcher'
    ) THEN
        CREATE ROLE mychandha_dispatcher
            NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
END
$$;

ALTER ROLE mychandha_api
    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
    NOINHERIT NOREPLICATION NOBYPASSRLS;
ALTER ROLE mychandha_dispatcher
    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
    NOINHERIT NOREPLICATION NOBYPASSRLS;

\if :{?api_login_role}
  SELECT CASE
      WHEN :'api_login_role' ~ '^[a-z_][a-z0-9_]{0,62}$' THEN 1
      ELSE CAST('invalid api_login_role' AS INTEGER)
  END;
  SELECT CASE
      WHEN EXISTS (
          SELECT 1 FROM pg_roles
          WHERE rolname = :'api_login_role'
            AND rolcanlogin
            AND NOT rolsuper
            AND NOT rolcreatedb
            AND NOT rolcreaterole
            AND NOT rolreplication
            AND NOT rolbypassrls
      ) THEN 1
      ELSE CAST('api login role is missing or privileged' AS INTEGER)
  END;
  SELECT format('GRANT mychandha_api TO %I', :'api_login_role')
  \gexec
\else
  \echo 'api_login_role not supplied; API membership was not changed'
\endif

\if :{?dispatcher_login_role}
  SELECT CASE
      WHEN :'dispatcher_login_role' ~ '^[a-z_][a-z0-9_]{0,62}$' THEN 1
      ELSE CAST('invalid dispatcher_login_role' AS INTEGER)
  END;
  SELECT CASE
      WHEN EXISTS (
          SELECT 1 FROM pg_roles
          WHERE rolname = :'dispatcher_login_role'
            AND rolcanlogin
            AND NOT rolsuper
            AND NOT rolcreatedb
            AND NOT rolcreaterole
            AND NOT rolreplication
            AND NOT rolbypassrls
      ) THEN 1
      ELSE CAST('dispatcher login role is missing or privileged' AS INTEGER)
  END;
  SELECT format(
      'GRANT mychandha_dispatcher TO %I',
      :'dispatcher_login_role'
  )
  \gexec
\else
  \echo 'dispatcher_login_role not supplied; dispatcher membership was not changed'
\endif
