package com.erp.admin.wms.service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationTransferServiceTest {

	@Test
	void standardInventoryMovesOnlyToStandardTemporaryOrVirtual() {
		assertTrue(LocationTransferService.isMoveAllowed("STANDARD", false, "STANDARD", false));
		assertTrue(LocationTransferService.isMoveAllowed("STANDARD", false, "TEMP", false));
		assertTrue(LocationTransferService.isMoveAllowed("STANDARD", false, "VIRTUAL", true));
		assertFalse(LocationTransferService.isMoveAllowed("STANDARD", false, "RETURN", false));
		assertFalse(LocationTransferService.isMoveAllowed("STANDARD", false, "DEFECTIVE", false));
	}

	@Test
	void returnInventoryCanBeOrganizedIntoNormalStorage() {
		assertTrue(LocationTransferService.isMoveAllowed("RETURN", false, "RETURN", false));
		assertTrue(LocationTransferService.isMoveAllowed("RETURN", false, "STANDARD", false));
		assertTrue(LocationTransferService.isMoveAllowed("RETURN", false, "TEMP", false));
		assertTrue(LocationTransferService.isMoveAllowed("RETURN", false, "VIRTUAL", true));
		assertFalse(LocationTransferService.isMoveAllowed("RETURN", false, "DEFECTIVE", false));
	}

	@Test
	void temporaryAndVirtualInventoryCannotEnterReturnOrDefectiveZones() {
		assertTrue(LocationTransferService.isMoveAllowed("TEMP", false, "STANDARD", false));
		assertTrue(LocationTransferService.isMoveAllowed("TEMP", false, "TEMP", false));
		assertTrue(LocationTransferService.isMoveAllowed("TEMP", false, "VIRTUAL", true));
		assertFalse(LocationTransferService.isMoveAllowed("TEMP", false, "RETURN", false));
		assertFalse(LocationTransferService.isMoveAllowed("TEMP", false, "DEFECTIVE", false));

		assertTrue(LocationTransferService.isMoveAllowed("VIRTUAL", true, "STANDARD", false));
		assertTrue(LocationTransferService.isMoveAllowed("VIRTUAL", true, "TEMP", false));
		assertTrue(LocationTransferService.isMoveAllowed("VIRTUAL", true, "VIRTUAL", true));
		assertFalse(LocationTransferService.isMoveAllowed("VIRTUAL", true, "RETURN", false));
		assertFalse(LocationTransferService.isMoveAllowed("VIRTUAL", true, "DEFECTIVE", false));
	}

	@Test
	void defectiveInventoryCannotMoveAndTemporaryInventoryIsNotAllocatable() {
		assertFalse(LocationTransferService.isMoveAllowed("DEFECTIVE", false, "DEFECTIVE", false));
		assertFalse(LocationTransferService.isMoveAllowed("DEFECTIVE", false, "STANDARD", false));
		assertFalse(LocationTransferService.isMoveAllowed("DEFECTIVE", false, "VIRTUAL", true));

		assertTrue(LocationTransferService.isAllocatableTarget("STANDARD"));
		assertTrue(LocationTransferService.isAllocatableTarget("RETURN"));
		assertFalse(LocationTransferService.isAllocatableTarget("TEMP"));
		assertFalse(LocationTransferService.isAllocatableTarget("DEFECTIVE"));
	}

}
