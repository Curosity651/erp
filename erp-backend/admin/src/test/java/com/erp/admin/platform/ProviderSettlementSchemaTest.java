package com.erp.admin.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSettlementSchemaTest {

    @Test
    void settlement_migration_adds_review_audit_and_unifies_platform_menu() throws IOException {
        Path migration = locate(Paths.get("sql", "migration", "V133__unify_provider_settlement.sql"));
        assertThat(migration).exists();

        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);
        assertThat(sql).contains("reviewer_id", "reviewer_name");
        assertThat(sql).contains("服务商结算", "platform-finance/provider-funds/index");
        assertThat(sql).contains("platform-finance:settle");
        assertThat(sql).contains("UPDATE sys_menu SET hidden=1 WHERE id=170601");
    }

    @Test
    void menu_title_encoding_has_a_utf8_safe_repair_migration() throws IOException {
        Path migration = locate(Paths.get("sql", "migration",
                "V134__repair_provider_settlement_menu_encoding.sql"));
        assertThat(migration).exists();

        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);
        assertThat(sql).contains("E69C8DE58AA1E59586E7BB93E7AE97");
        assertThat(sql).contains("WHERE id = 170603");
    }

    private Path locate(Path relative) {
        Path[] candidates = {
                relative,
                Paths.get("erp-backend").resolve(relative),
                Paths.get("..").resolve(relative),
                Paths.get("..", "..").resolve(relative)
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) return candidate;
        }
        return candidates[0];
    }
}
