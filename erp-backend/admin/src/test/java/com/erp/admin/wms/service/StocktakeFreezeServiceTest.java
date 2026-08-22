package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StocktakeMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StocktakeFreezeServiceTest {

	private final StocktakeMapper mapper = mock(StocktakeMapper.class);

	private final StocktakeFreezeService service = new StocktakeFreezeService(mapper);

	@Test
	void mutableLocationPassesWhenNoActiveStocktakeTaskExists() {
		when(mapper.countActiveLocationFreeze(1L, "A01-01")).thenReturn(0);

		assertDoesNotThrow(() -> service.assertLocationMutable(1L, "A01-01"));
		verify(mapper).countActiveLocationFreeze(1L, "A01-01");
	}

	@Test
	void activeStocktakeTaskBlocksInventoryMutation() {
		when(mapper.countActiveLocationFreeze(1L, "A01-01")).thenReturn(1);

		assertThrows(IllegalArgumentException.class,
				() -> service.assertLocationMutable(1L, "A01-01"));
	}

	@Test
	void missingLocationIsIgnoredForNonLocationInventory() {
		assertDoesNotThrow(() -> service.assertLocationMutable(1L, null));
	}

}
