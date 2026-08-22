package com.erp.admin.wms;

import java.math.BigDecimal;

import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsProductSnapshotModelTest {

	@Test
	void fulfillment_create_command_carries_immutable_product_snapshot() {
		FulfillmentCreateCommand command = new FulfillmentCreateCommand();
		command.setLogisticsProductId(7L);
		command.setLogisticsProductCode("ECONOMY");
		command.setLogisticsProductName("经济派送");
		command.setLogisticsProductDescription("3-7 个工作日，仓库选择实际承运方式");
		command.setLogisticsProductDefaultFee(new BigDecimal("35.00"));
		command.setLogisticsProductActualFee(new BigDecimal("35.00"));
		command.setLogisticsProductCurrency("RUB");

		assertThat(command.getLogisticsProductId()).isEqualTo(7L);
		assertThat(command.getLogisticsProductCode()).isEqualTo("ECONOMY");
		assertThat(command.getLogisticsProductName()).isEqualTo("经济派送");
		assertThat(command.getLogisticsProductDescription()).contains("仓库选择");
		assertThat(command.getLogisticsProductDefaultFee()).isEqualByComparingTo("35.00");
		assertThat(command.getLogisticsProductActualFee()).isEqualByComparingTo("35.00");
		assertThat(command.getLogisticsProductCurrency()).isEqualTo("RUB");
	}
}
