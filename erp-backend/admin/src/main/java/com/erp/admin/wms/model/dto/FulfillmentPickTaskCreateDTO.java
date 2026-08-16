package com.erp.admin.wms.model.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class FulfillmentPickTaskCreateDTO {
	@NotEmpty(message = "请选择待拣订单")
	private List<Long> fulfillmentOrderIds;
}
