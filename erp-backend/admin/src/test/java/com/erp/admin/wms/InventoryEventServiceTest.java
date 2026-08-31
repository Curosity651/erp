package com.erp.admin.wms;

import com.erp.admin.wms.mapper.InventoryEventLineMapper;
import com.erp.admin.wms.mapper.InventoryEventMapper;
import com.erp.admin.wms.model.dto.InventoryMutationContext;
import com.erp.admin.wms.model.dto.InventoryMutationLine;
import com.erp.admin.wms.model.entity.InventoryEvent;
import com.erp.admin.wms.model.entity.InventoryEventLine;
import com.erp.admin.wms.model.enums.InventoryEventType;
import com.erp.admin.wms.service.InventoryEventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryEventServiceTest {
	@Test
	void append_writes_header_and_signed_line_values() {
		InventoryEventMapper eventMapper = mock(InventoryEventMapper.class);
		InventoryEventLineMapper lineMapper = mock(InventoryEventLineMapper.class);
		when(eventMapper.insert(any())).thenAnswer(invocation -> {
			((InventoryEvent) invocation.getArgument(0)).setId(18L);
			return 1;
		});
		when(lineMapper.insert(any())).thenReturn(1);
		InventoryEventService service = new InventoryEventService(eventMapper, lineMapper);

		service.append(context("putaway:10:1"), 1L, 5L, 6L, 9L,
				Collections.singletonList(line(21L, 2L, 4, 0, 4, 0)));

		ArgumentCaptor<InventoryEventLine> captor = ArgumentCaptor.forClass(InventoryEventLine.class);
		verify(lineMapper).insert(captor.capture());
		assertThat(captor.getValue().getEventId()).isEqualTo(18L);
		assertThat(captor.getValue().getQuantityDelta()).isEqualTo(4);
		assertThat(captor.getValue().getAfterQuantity()).isEqualTo(4);
	}

	@Test
	void append_rejects_an_existing_idempotency_key() {
		InventoryEventMapper eventMapper = mock(InventoryEventMapper.class);
		when(eventMapper.selectByIdempotencyKey(1L, "same-key")).thenReturn(new InventoryEvent());
		InventoryEventService service = new InventoryEventService(eventMapper, mock(InventoryEventLineMapper.class));

		assertThatThrownBy(() -> service.append(context("same-key"), 1L, 5L, 6L, 9L,
				Collections.singletonList(line(21L, 2L, 4, 0, 4, 0))))
				.hasMessageContaining("请勿重复提交");
	}

	private InventoryMutationContext context(String key) {
		return InventoryMutationContext.builder().eventType(InventoryEventType.INBOUND_PUTAWAY)
				.sourceType("INBOUND").sourceId(10L).sourceNo("IN-10").idempotencyKey(key).build();
	}

	private InventoryMutationLine line(Long inventoryId, Long locationId, int delta, int before, int after,
			int reserved) {
		return InventoryMutationLine.builder().inventoryId(inventoryId).locationId(locationId).skuCode("SKU-A")
				.quality("GOOD").quantityDelta(delta).reservedDelta(0).beforeQuantity(before)
				.afterQuantity(after).beforeReserved(reserved).afterReserved(reserved).build();
	}
}
