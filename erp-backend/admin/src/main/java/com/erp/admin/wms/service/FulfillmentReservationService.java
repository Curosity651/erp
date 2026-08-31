package com.erp.admin.wms.service;

import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FulfillmentReservationService {

	private final LocationInventoryService inventoryService;

	public void reserve(Long fulfillmentId, FulfillmentCreateCommand command,
			List<WmsFulfillmentItem> persistedItems) {
		for (WmsFulfillmentItem item : persistedItems) {
			InventoryReservationRequest request = new InventoryReservationRequest();
			request.setFulfillmentOrderId(fulfillmentId);
			request.setFulfillmentItemId(item.getId());
			request.setTenantId(command.getTenantId());
			request.setWmsTenantId(command.getWmsTenantId());
			request.setErpTenantId(command.getErpTenantId());
			request.setWarehouseId(command.getWarehouseId());
			request.setSkuCode(item.getSkuCode());
			request.setQuality(item.getQuality());
			request.setQuantity(item.getQuantity());
			inventoryService.reserve(request, com.erp.admin.wms.model.dto.InventoryMutationContext.builder()
					.eventType(com.erp.admin.wms.model.enums.InventoryEventType.RESERVE)
					.sourceType("FULFILLMENT").sourceId(fulfillmentId)
					.sourceNo(command.getSourceOrderNo()).reason("销售订单库存预占")
					.idempotencyKey("fulfillment-reserve:" + fulfillmentId + ":" + item.getId()).build());
		}
	}

	public void release(Long fulfillmentId) {
		inventoryService.release(fulfillmentId, com.erp.admin.wms.model.dto.InventoryMutationContext.builder()
				.eventType(com.erp.admin.wms.model.enums.InventoryEventType.RELEASE)
				.sourceType("FULFILLMENT").sourceId(fulfillmentId).reason("取消履约释放库存")
				.idempotencyKey("fulfillment-release:" + fulfillmentId).build());
	}
}
