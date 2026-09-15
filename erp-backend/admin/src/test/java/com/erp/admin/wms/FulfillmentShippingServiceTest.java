package com.erp.admin.wms;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

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
import com.erp.admin.wms.service.platform.PlatformActionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentShippingServiceTest {

	private WmsFulfillmentOrderMapper orderMapper;
	private FulfillmentPickingService pickingService;
	private FulfillmentPlatformActionService platformActions;
	private FulfillmentShippingService service;
	private WmsFulfillmentItemMapper itemMapper;
	private LocationInventoryService inventoryService;
	private WarehouseBillingService billingService;
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		orderMapper = mock(WmsFulfillmentOrderMapper.class);
		pickingService = mock(FulfillmentPickingService.class);
		platformActions = mock(FulfillmentPlatformActionService.class);
		itemMapper = mock(WmsFulfillmentItemMapper.class);
		inventoryService = mock(LocationInventoryService.class);
		billingService = mock(WarehouseBillingService.class);
		transactionTemplate = mock(TransactionTemplate.class);
		service = new FulfillmentShippingService(orderMapper, itemMapper,
				mock(FulfillmentProgressService.class), mock(PlatformLabelVerificationService.class),
				platformActions, inventoryService, billingService, transactionTemplate);
		ReflectionTestUtils.setField(service, "pickingService", pickingService);
	}

	@Test
	void successful_ship_records_the_actual_operator() {
		WmsFulfillmentOrder order = new WmsFulfillmentOrder();
		order.setId(1L);
		order.setFulfillmentStatus(FulfillmentStatus.PACKED);
		when(orderMapper.selectById(1L)).thenReturn(order);
		when(orderMapper.selectForUpdate(1L)).thenReturn(order);
		when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(orderMapper.transit(1L, FulfillmentStatus.PACKED, FulfillmentStatus.SHIPPED)).thenReturn(1);
		doAnswer(invocation -> {
			java.util.function.Consumer<?> callback = invocation.getArgument(0);
			@SuppressWarnings("unchecked")
			java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action =
					(java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) callback;
			action.accept(mock(org.springframework.transaction.TransactionStatus.class));
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());
		ReflectionTestUtils.setField(service, "logisticsFeeService",
				mock(com.erp.admin.wms.service.FulfillmentLogisticsFeeService.class));

		service.ship(Collections.singletonList(1L), 99L);

		org.assertj.core.api.Assertions.assertThat(order.getShippedBy()).isEqualTo(99L);
		verify(orderMapper).updateById(order);
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
	void simplified_task_completion_reuses_ready_and_pack_flow() {
		WmsFulfillmentOrder order = waitingPackOrder();
		order.setSourceType("OZON");
		order.setLabelBarcode("TRACK-1");
		order.setLabelVerifiedTime(null);
		order.setLogisticsProductName("标准物流服务");
		when(pickingService.completeSimplifiedPicking(11L,
				Collections.singletonList(801L), 99L)).thenReturn(Collections.singletonList(1L));
		when(orderMapper.selectById(1L)).thenReturn(order);
		when(platformActions.markReady(1L)).thenReturn(PlatformActionResult.success("OK", "TRACK-1"));
		when(orderMapper.transit(1L, FulfillmentStatus.WAITING_PACK,
				FulfillmentStatus.PACKED)).thenReturn(1);

		service.completeSimplifiedTask(11L, Collections.singletonList(801L), 99L);

		verify(platformActions).markReady(1L);
		verify(pickingService).completePackedOrder(1L);
		verify(pickingService).markSimplifiedCompleted(11L, 99L);
		org.assertj.core.api.Assertions.assertThat(order.getCarrierName()).isEqualTo("OZON");
		org.assertj.core.api.Assertions.assertThat(order.getShippingMethod())
				.isEqualTo("标准物流服务");
		org.assertj.core.api.Assertions.assertThat(order.getTrackingNo()).isEqualTo("TRACK-1");
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
