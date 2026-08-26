package com.erp.admin.finance.settlement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerFundLedgerControllerContractTest {

    @Test
    void owner_exposes_monthly_ledger_and_date_range_export() throws IOException {
        Path source = locate(Paths.get("admin", "src", "main", "java", "com", "erp", "admin",
                "finance", "settlement", "controller", "OwnerFundSettlementController.java"));
        String java = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertThat(java).contains("@GetMapping(\"/ledger/months\")");
        assertThat(java).contains("queryService.ownerLedgerMonths(qo)");
        assertThat(java).contains("@GetMapping(\"/ledger/export\")");
        assertThat(java).contains("ledgerExportService.export(queryService.ownerLedgerMonths(qo))");
    }

    private Path locate(Path relative) {
        Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative),
                Paths.get("..").resolve(relative) };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) return candidate;
        }
        return candidates[0];
    }
}
