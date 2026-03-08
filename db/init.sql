CREATE DATABASE IF NOT EXISTS spread_input
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE spread_input;

CREATE TABLE IF NOT EXISTS students (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_students_name (name)
);

CREATE TABLE IF NOT EXISTS app_settings (
  id BIGINT NOT NULL,
  spreadsheet_id VARCHAR(255) NOT NULL,
  sheet_name VARCHAR(100) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
  token VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  revoked BIT(1) NOT NULL,
  PRIMARY KEY (token)
);

INSERT INTO app_settings (id, spreadsheet_id, sheet_name, updated_at)
SELECT 1, '', '관찰', NOW()
WHERE NOT EXISTS (SELECT 1 FROM app_settings WHERE id = 1);
