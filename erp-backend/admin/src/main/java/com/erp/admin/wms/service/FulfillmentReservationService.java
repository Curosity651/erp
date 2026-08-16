package com.erp.admin.wms.service;

import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.dto.InventoryReservationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FulfillmentReservationService {

	private final LocationInventoryService inventoryService;

	public void reserve(Long fulfillmentId, FulfillmentCreateCommand command) {
		for (FulfillmentCreateCommand.Item item : command.getItems()) {
			InventoryReservationRequest request = new InventoryReservationRequest();
			request.setFulfillmentOrderId(fulfillmentId);
			request.setTenantId(command.getTenantId());
			request.setWmsTenantId(command.getWmsTenantId());
			request.setErpTenantId(command.getErpTenantId());
			request.setWarehouseId(command.getWarehouseId());
			request.setSkuCode(item.getSkuCode());
			request.setQuality(item.getQuality());
			request.setQuantity(item.getQuantity());
			inventoryService.reserve(request);
		}
	}

	public void release(Long fulfillmentId) {
		inventoryService.release(fulfillmentId);
	}
}
