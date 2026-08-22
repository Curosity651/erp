package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.WmsSalesOutboundPackageMapper;
import com.erp.admin.wms.mapper.WmsSortSlotMapper;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.erp.admin.wms.model.entity.WmsSortSlot;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsSortSlotServiceTest {

	@Test
	void reserves_distinct_fixed_slots_and_binds_packages() {
		WmsSortSlotMapper slotMapper = mock(WmsSortSlotMapper.class);
		WmsSalesOutboundPackageMapper packageMapper = mock(WmsSalesOutboundPackageMapper.class);
		WmsSortSlotService service = new WmsSortSlotService(slotMapper, packageMapper);
		WmsSortSlot b01 = slot(1L, "B01");
		WmsSortSlot b02 = slot(2L, "B02");
		when(slotMapper.selectAvailableForUpdate(9L, 2)).thenReturn(Arrays.asList(b01, b02));
		when(slotMapper.updateById(any())).thenReturn(1);
		when(packageMapper.updateById(any())).thenReturn(1);

		WmsSalesOutboundPackage first = pack(101L, 1001L);
		WmsSalesOutboundPackage second = pack(102L, 1002L);
		service.reserveForTask(88L, 9L, Arrays.asList(first, second));

		assertThat(first.getSortSlotId()).isEqualTo(1L);
		assertThat(first.getSortCode()).isEqualTo("B01");
		assertThat(second.getSortSlotId()).isEqualTo(2L);
		assertThat(b01.getSlotStatus()).isEqualTo(WmsSortSlotService.STATUS_RESERVED);
		assertThat(b01.getTaskId()).isEqualTo(88L);
		assertThat(b01.getPackageId()).isEqualTo(101L);
	}

	@Test
	void rejects_task_when_fixed_slots_are_insufficient() {
		WmsSortSlotMapper slotMapper = mock(WmsSortSlotMapper.class);
		WmsSortSlotService service = new WmsSortSlotService(slotMapper,
				mock(WmsSalesOutboundPackageMapper.class));
		when(slotMapper.selectAvailableForUpdate(9L, 2))
				.thenReturn(Collections.singletonList(slot(1L, "B01")));

		assertThatThrownBy(() -> service.reserveForTask(88L, 9L,
				Arrays.asList(pack(101L, 1001L), pack(102L, 1002L))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("可用分货格不足");
	}

	@Test
	void releasing_package_clears_binding_but_keeps_fixed_slot() {
		WmsSortSlotMapper slotMapper = mock(WmsSortSlotMapper.class);
		WmsSortSlotService service = new WmsSortSlotService(slotMapper,
				mock(WmsSalesOutboundPackageMapper.class));
		WmsSortSlot b01 = slot(1L, "B01");
		b01.setSlotStatus(WmsSortSlotService.STATUS_READY);
		b01.setTaskId(88L);
		b01.setPackageId(101L);
		when(slotMapper.selectByPackageIdForUpdate(101L)).thenReturn(b01);
		when(slotMapper.updateById(any())).thenReturn(1);

		service.releaseByPackage(101L);

		assertThat(b01.getSlotStatus()).isEqualTo(WmsSortSlotService.STATUS_AVAILABLE);
		assertThat(b01.getTaskId()).isNull();
		assertThat(b01.getPackageId()).isNull();
		verify(slotMapper).updateById(b01);
	}

	@Test
	void slot_follows_sort_pack_lifecycle() {
		WmsSortSlotMapper slotMapper = mock(WmsSortSlotMapper.class);
		WmsSortSlotService service = new WmsSortSlotService(slotMapper,
				mock(WmsSalesOutboundPackageMapper.class));
		WmsSortSlot b01 = slot(1L, "B01");
		b01.setSlotStatus(WmsSortSlotService.STATUS_RESERVED);
		b01.setTaskId(88L);
		b01.setPackageId(101L);
		when(slotMapper.selectByTaskIdForUpdate(88L)).thenReturn(Collections.singletonList(b01));
		when(slotMapper.selectByPackageIdForUpdate(101L)).thenReturn(b01);
		when(slotMapper.updateById(any())).thenReturn(1);

		service.markTaskSorting(88L);
		assertThat(b01.getSlotStatus()).isEqualTo(WmsSortSlotService.STATUS_SORTING);

		service.markReady(101L);
		assertThat(b01.getSlotStatus()).isEqualTo(WmsSortSlotService.STATUS_READY);

		service.markPacking(101L);
		assertThat(b01.getSlotStatus()).isEqualTo(WmsSortSlotService.STATUS_PACKING);
	}

	@Test
	void scan_code_locates_only_an_active_fixed_slot_binding() {
		WmsSortSlotMapper slotMapper = mock(WmsSortSlotMapper.class);
		WmsSortSlotService service = new WmsSortSlotService(slotMapper,
				mock(WmsSalesOutboundPackageMapper.class));
		WmsSortSlot b01 = slot(1L, "B01");
		b01.setSlotStatus(WmsSortSlotService.STATUS_READY);
		b01.setPackageId(101L);
		when(slotMapper.selectByScanCode("SORT:KDEF:B01")).thenReturn(b01);

		assertThat(service.locateActivePackage("SORT:KDEF:B01")).isEqualTo(101L);

		b01.setSlotStatus(WmsSortSlotService.STATUS_AVAILABLE);
		b01.setPackageId(null);
		assertThat(service.locateActivePackage("SORT:KDEF:B01")).isNull();
	}

	private static WmsSortSlot slot(Long id, String code) {
		WmsSortSlot slot = new WmsSortSlot();
		slot.setId(id);
		slot.setWarehouseId(9L);
		slot.setSlotCode(code);
		slot.setScanCode("SORT:KDEF:" + code);
		slot.setSlotStatus(WmsSortSlotService.STATUS_AVAILABLE);
		return slot;
	}

	private static WmsSalesOutboundPackage pack(Long id, Long outboundOrderId) {
		WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
		pack.setId(id);
		pack.setOutboundOrderId(outboundOrderId);
		return pack;
	}

}
