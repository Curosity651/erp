package com.erp.admin.wms;

import com.erp.admin.wms.service.AdjustmentService;
import com.erp.admin.wms.service.LocationInventoryService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LogicalInventoryOperationRulesTest {

	@Test
	void stocktake_count_cannot_drop_below_reserved_quantity() {
		LocationInventoryService.validateCountedQuantity(10, 4);
		assertThatIllegalArgumentException().isThrownBy(
				() -> LocationInventoryService.validateCountedQuantity(3, 4));
	}

	@Test
	void scrap_accepts_only_defective_stock_in_defective_zone() {
		AdjustmentService.validateScrapCandidate("DEFECTIVE", "DEFECTIVE");
		AdjustmentService.validateScrapCandidate("DAMAGED", "DEFECTIVE");
		assertThatIllegalArgumentException().isThrownBy(
				() -> AdjustmentService.validateScrapCandidate("GOOD", "DEFECTIVE"));
		assertThatIllegalArgumentException().isThrownBy(
				() -> AdjustmentService.validateScrapCandidate("DEFECTIVE", "STANDARD"));
	}

}
