package com.erp.admin.wms;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.erp.admin.platform.finance.service.WarehouseBillingService;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentPackDTO;
import com.erp.admin.wms.model.dto.FulfillmentLogisticsFeeDTO;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.service.FulfillmentPickingService;
import com.erp.admin.wms.service.FulfillmentPlatformActionService;
import com.erp.admin.wms.service.FulfillmentProgressService;
import com.erp.admin.wms.service.FulfillmentShippingService;
import com.erp.admin.wms.service.LocationInventoryService;
import com.erp.admin.wms.service.PlatformLabelVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentShippingServiceTest {

	private WmsFulfillmentOrderMapper orderMapper;
	private FulfillmentPickingService pickingService;
	private FulfillmentShippingService service;

	@BeforeEach
	void setUp() {
		orderMapper = mock(WmsFulfillmentOrderMapper.class);
		pickingService = mock(FulfillmentPickingService.class);
		service = new FulfillmentShippingService(orderMapper, mock(WmsFulfillmentItemMapper.class),
				mock(FulfillmentProgressService.class), mock(PlatformLabelVerificationService.class),
				mock(FulfillmentPlatformActionService.class), mock(LocationInventoryService.class),
				mock(WarehouseBillingService.class), mock(TransactionTemplate.class));
		ReflectionTestUtils.setField(service, "pickingService", pickingService);
	}

	@Test
	void pack_requires_tracking_number() {
		WmsFulfillmentOrder order = waitingPackOrder();
		when(orderMapper.selectById(1L)).thenReturn(order);
		FulfillmentPackDTO dto = validPack();
		dto.setTrackingNo(null);

		assertThatThrownBy(() -> service.pack(1L, dto, 99L)).hasMessageContaining("跟踪号");
	}

	@Test
	void successful_pack_unlocks_next_task_order() {
		WmsFulfillmentOrder order = waitingPackOrder();
		when(orderMapper.selectById(1L)).thenReturn(order);
		when(orderMapper.transit(1L, FulfillmentStatus.WAITING_PACK, FulfillmentStatus.PACKED)).thenReturn(1);

		service.pack(1L, validPack(), 99L);

		verify(pickingService).assertTaskOperator(1L, 99L);
		verify(pickingService).completePackedOrder(1L);
	}

	@Test
	void logistics_fee_can_only_be_adjusted_by_the_owning_wms_provider() {
		TenantIdentityService identityService = mock(TenantIdentityService.class);
		TenantIdentityVO identity = new TenantIdentityVO();
		identity.setIdentityType(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM);
		identity.setTenantId(-1L);
		when(identityService.currentIdentity(null)).thenReturn(identity);
		ReflectionTestUtils.setField(service, "tenantIdentityService", identityService);
		FulfillmentLogisticsFeeDTO dto = new FulfillmentLogisticsFeeDTO();
		dto.setAmount(new BigDecimal("30.00"));

		assertThatThrownBy(() -> service.adjustLogisticsFee(1L, dto))
				.hasMessageContaining("只有WMS服务商");
	}

	private WmsFulfillmentOrder waitingPackOrder() {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(1L);
		order.setFulfillmentStatus(FulfillmentStatus.WAITING_PACK);
		order.setLabelVerifiedTime(LocalDateTime.now());
		return order;
	}

	private FulfillmentPackDTO validPack() {
		FulfillmentPackDTO dto = new FulfillmentPackDTO();
		dto.setCarrierName("CDEK");
		dto.setShippingMethod("STANDARD");
		dto.setTrackingNo("TRACK-1");
		dto.setPackageWeightKg(new BigDecimal("2.5"));
		return dto;
	}
}
