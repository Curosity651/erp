package com.erp.admin.wms.service;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Verifies that the scanned platform label belongs to the current package.
 */
@Service
public class PlatformLabelVerificationService {

	private final ErpOrderService orderService;
	private final ObjectMapper objectMapper;

	public PlatformLabelVerificationService(ErpOrderService orderService,
			@Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
		this.orderService = orderService;
		this.objectMapper = objectMapper;
	}

	public boolean matches(WmsSalesOutboundPackage pack, String scanCode) {
		if (pack == null || !StringUtils.hasText(scanCode)) {
			return false;
		}
		String normalized = scanCode.trim();
		if (normalized.equals(pack.getPlatformOrderId())) {
			return true;
		}
		ErpOrder order = TenantContext.runAs(pack.getErpTenantId(),
				() -> orderService.getById(pack.getErpOrderId()));
		return matchesOrder(order, normalized);
	}

	public boolean matches(WmsFulfillmentOrder fulfillment, String scanCode) {
		if (fulfillment == null || !StringUtils.hasText(scanCode)) {
			return false;
		}
		String normalized = scanCode.trim();
		if (normalized.equals(fulfillment.getSourceOrderNo())
				|| normalized.equals(fulfillment.getLabelBarcode())) {
			return true;
		}
		if (fulfillment.getSourceOrderId() == null || fulfillment.getErpTenantId() == null) {
			return false;
		}
		ErpOrder order = TenantContext.runAs(fulfillment.getErpTenantId(),
				() -> orderService.getById(fulfillment.getSourceOrderId()));
		return matchesOrder(order, normalized);
	}

	private boolean matchesOrder(ErpOrder order, String normalized) {
		if (order == null) {
			return false;
		}
		if (normalized.equals(order.getPlatformOrderId())
				|| normalized.equals(order.getShipmentId())) {
			return true;
		}
		for (String code : parseCodes(order.getLabelVerifyCodes())) {
			if (normalized.equals(code)) {
				return true;
			}
		}
		return false;
	}

	private List<String> parseCodes(String json) {
		if (!StringUtils.hasText(json)) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<String>>() { });
		}
		catch (Exception ignored) {
			return Collections.emptyList();
		}
	}
}
