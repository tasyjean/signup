# --- First database schema
# --- For heroku deployment - use PostgreSQL - see http://www.postgresql.org/
# --- Must also work with H2 - test locally
# ---
# --- Password is 'admin'

# --- !Ups

ALTER TABLE groups DROP COLUMN smtp_password;
ALTER TABLE groups DROP COLUMN smtp_user;
ALTER TABLE groups DROP COLUMN smtp_useTls;
ALTER TABLE groups DROP COLUMN smtp_useSsl;
ALTER TABLE groups DROP COLUMN smtp_port;
ALTER TABLE groups DROP COLUMN smtp_host;

ALTER TABLE groups ADD COLUMN mail_from VARCHAR(127) DEFAULT '';
UPDATE groups SET mail_from=smtp_from;
ALTER TABLE groups DROP COLUMN smtp_from;

# --- !Downs

ALTER TABLE groups ADD COLUMN smtp_host VARCHAR(127) DEFAULT '';
ALTER TABLE groups ADD COLUMN smtp_port INTEGER DEFAULT 25;
ALTER TABLE groups ADD COLUMN smtp_useSsl BOOLEAN DEFAULT FALSE;
ALTER TABLE groups ADD COLUMN smtp_useTls BOOLEAN DEFAULT FALSE;
ALTER TABLE groups ADD COLUMN smtp_user VARCHAR(127) DEFAULT '';
ALTER TABLE groups ADD COLUMN smtp_password VARCHAR(127) DEFAULT '';

ALTER TABLE groups ADD COLUMN smtp_from VARCHAR(127) DEFAULT '';
UPDATE groups SET smtp_from=mail_from;
ALTER TABLE groups DROP COLUMN mail_from;
