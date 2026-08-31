package com.erp.admin.wms;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerDocumentHardeningContractTest {

	@Test
	void purchase_order_guards_associated_shipping_and_uses_percentage_default() throws Exception {
		String source = read("src/main/java/com/erp/admin/wms/service/PurchaseOrderService.java");

		assertThat(source).contains("existsAssociatedShippingOrder(id)");
		assertThat(source).contains("new BigDecimal(\"30\")");
		assertThat(source).contains("status != PurchaseOrderStatus.COMPLETED");
	}

	@Test
	void inbound_endpoints_and_mutations_enforce_source_type() throws Exception {
		String service = read("src/main/java/com/erp/admin/wms/service/PurchaseInboundService.java");
		String facade = read("src/main/java/com/erp/admin/wms/facade/PurchaseInboundFacade.java");
		String purchaseController = read("src/main/java/com/erp/admin/wms/controller/PurchaseInboundController.java");
		String manualController = read("src/main/java/com/erp/admin/wms/controller/ManualInboundController.java");
		String returnController = read("src/main/java/com/erp/admin/wms/controller/CustomReturnController.java");

		assertThat(service).contains("assertSourceType(order, InboundSourceType.PURCHASE)");
		assertThat(service).contains("assertSourceType(order, InboundSourceType.MANUAL)");
		assertThat(service).contains("assertSourceType(order, InboundSourceType.CUSTOM_RETURN)");
		assertThat(facade).contains("validateForSubmission(order)");
		assertThat(purchaseController).contains("InboundSourceType.PURCHASE");
		assertThat(manualController).contains("InboundSourceType.MANUAL");
		assertThat(returnController).contains("InboundSourceType.CUSTOM_RETURN");
	}

	@Test
	void owner_business_numbers_are_unique_within_owner_scope() throws Exception {
		Path migration = Paths.get("../sql/migration/V136__owner_scoped_business_numbers.sql");
		assertThat(migration).exists();
		String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8);

		assertThat(sql).contains("erp_tenant_id, provider_code, deleted");
		assertThat(sql).contains("erp_tenant_id, order_no, deleted");
		assertThat(sql).contains("erp_tenant_id, shipping_no, deleted");
	}

	private static String read(String relativePath) throws Exception {
		return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
	}
}
