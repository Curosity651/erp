package com.erp.admin.wms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewInventoryFeatureDependencyGuardTest {

	private static final List<String> RETIRED_DEPENDENCIES = Arrays.asList(
			"import com.erp.admin.wms.mapper.InventoryMapper;",
			"import com.erp.admin.wms.service.InventoryService;", "wms_inventory",
			"wms_stock_flow", "wms_stock_posting", "wms_physical_inventory");

	@Test
	void inventory_features_do_not_depend_on_retired_inventory_runtime() throws IOException {
		assertClean("admin/src/main/java/com/erp/admin/wms/controller/InventoryController.java");
		assertClean("admin/src/main/java/com/erp/admin/wms/service/RegionStockDataProvider.java");
		assertClean("admin/src/main/java/com/erp/admin/wms/facade/InventoryForecastFacade.java");
		assertClean("admin/src/main/java/com/erp/admin/wms/controller/StockFlowController.java");
		assertClean("admin/src/main/java/com/erp/admin/wms/controller/StockPostingQueryController.java");
	}

	private void assertClean(String relative) throws IOException {
		String source = new String(Files.readAllBytes(locate(Paths.get(relative))), StandardCharsets.UTF_8);
		for (String retired : RETIRED_DEPENDENCIES) {
			assertThat(source).as("%s must not reference %s", relative, retired).doesNotContain(retired);
		}
	}

	private Path locate(Path relative) {
		Path[] candidates = { relative, Paths.get("erp-backend").resolve(relative),
				Paths.get("..").resolve(relative), Paths.get("..", "..").resolve(relative) };
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) return candidate;
		}
		return candidates[0];
	}
}
