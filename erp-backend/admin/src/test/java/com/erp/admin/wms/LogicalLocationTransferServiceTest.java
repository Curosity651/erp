package com.erp.admin.wms;

import java.util.Collections;

import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationInventory;
import com.erp.admin.wms.service.LogicalLocationTransferService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LogicalLocationTransferServiceTest {

	@Test
	void normal_and_public_temp_locations_accept_good_stock() {
		WmsLocation standard = location("STANDARD", "A1", 0);
		WmsLocation temp = location("TEMP", "TMP", 1);

		LogicalLocationTransferService.validateMovement("GOOD", standard, "STANDARD");
		LogicalLocationTransferService.validateMovement("GOOD", temp, "TEMP");
		LogicalLocationTransferService.validateTargetAccess(standard, Collections.singleton("A1"));
		LogicalLocationTransferService.validateTargetAccess(temp, Collections.emptySet());
	}

	@Test
	void defective_stock_cannot_be_transferred() {
		WmsLocation target = location("STANDARD", "A1", 0);

		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalLocationTransferService.validateMovement("DEFECTIVE", target, "STANDARD"));
		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalLocationTransferService.validateMovement("DAMAGED", target, "STANDARD"));
	}

	@Test
	void defective_zone_rejects_good_stock() {
		WmsLocation target = location("DEFECTIVE", "D1", 0);

		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalLocationTransferService.validateMovement("GOOD", target, "DEFECTIVE"));
	}

	@Test
	void reserved_quantity_cannot_be_moved() {
		WmsLocationInventory inventory = new WmsLocationInventory();
		inventory.setQuantity(10);
		inventory.setReservedQuantity(4);

		LogicalLocationTransferService.validateMovableQuantity(inventory, 6);
		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalLocationTransferService.validateMovableQuantity(inventory, 7));
	}

	@Test
	void private_location_requires_service_provider_rack_assignment() {
		WmsLocation target = location("STANDARD", "B2", 0);

		assertThatIllegalArgumentException().isThrownBy(
				() -> LogicalLocationTransferService.validateTargetAccess(target, Collections.singleton("A1")));
	}

	private WmsLocation location(String type, String rackNo, int publicShared) {
		WmsLocation location = new WmsLocation();
		location.setLocationType(type);
		location.setRackNo(rackNo);
		location.setPublicShared(publicShared);
		return location;
	}

}
