package com.erp.admin.platform.wildberries.model.request.supply;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WbAddOrderToSupplierRequest {

	@JsonProperty("orders")
	private List<Long> orders;

}
