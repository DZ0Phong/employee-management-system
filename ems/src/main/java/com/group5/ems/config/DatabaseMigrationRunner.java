package com.group5.ems.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Code-first DB migration runner.
 * Idempotent — safe to run on every startup.
 * Runs before the application is fully ready so schema is always in sync.
 */
@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /** Valid status values for the users.status ENUM. */
    private static final String STATUS_ENUM =
            "enum('ACTIVE','INACTIVE','LOCKED','LOCK5')";

    @Override
    public void run(ApplicationArguments args) {
        // ── 1. Add new columns (idempotent) ─────────────────────────────
        addColumnIfNotExists("users", "failed_login_count",
                "ALTER TABLE users ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0");

        addColumnIfNotExists("users", "locked_until",
                "ALTER TABLE users ADD COLUMN locked_until DATETIME NULL");

        addColumnIfNotExists("users", "activation_otp",
                "ALTER TABLE users ADD COLUMN activation_otp VARCHAR(6) NULL");

        addColumnIfNotExists("users", "activation_otp_expires_at",
                "ALTER TABLE users ADD COLUMN activation_otp_expires_at DATETIME NULL");

        // ── 2. Enforce status ENUM in DB ─────────────────────────────────
        enforceStatusEnum();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void addColumnIfNotExists(String table, String column, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
                System.out.println("[Migration] Added column: " + table + "." + column);
            }
        } catch (Exception e) {
            System.err.println("[Migration] Could not add column " + table + "." + column
                    + ": " + e.getMessage());
        }
    }

    /**
     * Migrates users.status to ENUM('ACTIVE','INACTIVE','LOCKED','LOCK5').
     *
     * Steps:
     *   1. Sanitize any unrecognised status values → INACTIVE (safe default).
     *   2. Check current column type.
     *   3. If not already the target ENUM, ALTER the column.
     */
    private void enforceStatusEnum() {
        try {
            // Step 1 — sanitize unrecognised values before altering to ENUM
            //   (ALTER TABLE would reject rows with values outside the enum set)
            jdbcTemplate.execute(
                    "UPDATE users SET status = 'INACTIVE' " +
                    "WHERE status NOT IN ('ACTIVE','INACTIVE','LOCKED','LOCK5')");

            // Step 2 — check current column type
            String currentType = jdbcTemplate.queryForObject(
                    "SELECT LOWER(COLUMN_TYPE) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "  AND TABLE_NAME   = 'users' " +
                    "  AND COLUMN_NAME  = 'status'",
                    String.class);

            if (currentType != null && currentType.contains("enum")
                    && currentType.contains("lock5") && currentType.contains("locked")) {
                // Already the correct ENUM — nothing to do
                return;
            }

            // Step 3 — alter column to ENUM
            jdbcTemplate.execute(
                    "ALTER TABLE users MODIFY COLUMN status " +
                    "ENUM('ACTIVE','INACTIVE','LOCKED','LOCK5') " +
                    "NOT NULL DEFAULT 'ACTIVE'");

            System.out.println("[Migration] users.status converted to ENUM with values: " +
                    "ACTIVE, INACTIVE, LOCKED, LOCK5");

        } catch (Exception e) {
            System.err.println("[Migration] Could not enforce status ENUM: " + e.getMessage());
        }
    }
}
