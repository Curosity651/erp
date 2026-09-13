package com.erp.admin.wms.model.dto;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundHandoverCreateDTO extends FulfillmentHandoverDTO {
	@NotNull(message = "请选择已打包货物")
	private Long fulfillmentOrderId;
}
