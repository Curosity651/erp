package com.erp.admin.wms;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.erp.admin.platform.finance.mapper.WmsContractRackMapper;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.mapper.WmsRackAssignmentMapper;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.vo.RackVO;
import com.erp.admin.wms.service.WmsRackAssignmentService;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 货架分配纯逻辑测试（C2）：时间段重叠、有效、临期。
 *
 * @author erp
 */
class WmsRackAssignmentServiceTest {

	@Test
	void overlapping_periods_detected() {
		LocalDate jan1 = LocalDate.of(2026, 1, 1);
		LocalDate jun30 = LocalDate.of(2026, 6, 30);
		LocalDate mar1 = LocalDate.of(2026, 3, 1);
		LocalDate dec31 = LocalDate.of(2026, 12, 31);
		// [1/1,6/30] 与 [3/1,12/31] 重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, jun30, mar1, dec31)).isTrue();
		// 开放结束日（null）与任何之后的段重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, null, dec31, null)).isTrue();
	}

	@Test
	void non_overlapping_periods() {
		LocalDate jan1 = LocalDate.of(2026, 1, 1);
		LocalDate mar31 = LocalDate.of(2026, 3, 31);
		LocalDate apr1 = LocalDate.of(2026, 4, 1);
		LocalDate jun30 = LocalDate.of(2026, 6, 30);
		// [1/1,3/31] 与 [4/1,6/30] 不重叠
		assertThat(WmsRackAssignmentService.periodsOverlap(jan1, mar31, apr1, jun30)).isFalse();
	}

	@Test
	void active_judgement() {
		LocalDate today = LocalDate.of(2026, 6, 28);
		// 已开始、未结束
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 1, 1), null, today)).isTrue();
		// 未开始
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 7, 1), null, today)).isFalse();
		// 已结束
		assertThat(WmsRackAssignmentService.isActive(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1), today))
			.isFalse();
	}

	@Test
	void expiring_soon_within_30_days() {
		LocalDate today = LocalDate.of(2026, 6, 1);
		// 6/20 距今 19 天 → 临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(LocalDate.of(2026, 6, 20), today)).isTrue();
		// 8/1 距今 > 30 天 → 不临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(LocalDate.of(2026, 8, 1), today)).isFalse();
		// 无结束日 → 不临期
		assertThat(WmsRackAssignmentService.isExpiringSoon(null, today)).isFalse();
	}

	@Test
	void rack_numbers_are_sorted_naturally() {
		ArrayList<String> rackNos = new ArrayList<>(Arrays.asList("A10", "A2", "A11", "A1", "A3"));

		rackNos.sort(WmsRackAssignmentService::compareRackNo);

		assertThat(rackNos).containsExactly("A1", "A2", "A3", "A10", "A11");
	}

	@Test
	void preview_uses_actual_non_shared_logical_locations_without_legacy_generation_state() {
		WmsLocationMapper locationMapper = mock(WmsLocationMapper.class);
		WmsPhysicalInventoryMapper inventoryMapper = mock(WmsPhysicalInventoryMapper.class);
		WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
		SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
		PrincipalAttributeAccessor principalAccessor = mock(PrincipalAttributeAccessor.class);
		WmsContractRackMapper contractRackMapper = mock(WmsContractRackMapper.class);
		WmsRackAssignmentMapper assignmentMapper = mock(WmsRackAssignmentMapper.class);
		WmsRackAssignmentService service = new WmsRackAssignmentService(locationMapper, inventoryMapper,
				warehouseMapper, tenantMapper, principalAccessor, contractRackMapper);
		ReflectionTestUtils.setField(service, "baseMapper", assignmentMapper);

		when(locationMapper.listAssignableByWarehouse(53L)).thenReturn(Arrays.asList(
				location("A1", 0), location("A2", 0)));
		when(assignmentMapper.listByWarehouse(53L)).thenReturn(Collections.emptyList());
		when(contractRackMapper.listCurrentBindingsByWarehouse(53L)).thenReturn(Collections.emptyList());

		List<RackVO> preview = service.preview(53L, null, null, null);

		assertThat(preview).extracting(RackVO::getRackNo).containsExactly("A1", "A2");
	}

	private WmsLocation location(String rackNo, int publicShared) {
		WmsLocation location = new WmsLocation();
		location.setRackNo(rackNo);
		location.setPublicShared(publicShared);
		location.setIsVirtual(0);
		return location;
	}

}
