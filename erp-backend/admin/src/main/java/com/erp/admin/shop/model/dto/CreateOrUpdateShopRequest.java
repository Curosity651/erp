package com.erp.admin.shop.model.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CreateOrUpdateShopRequest {

	private String platform;

	private Map<String, String> credential;

	private String shopName;

	private String erpShopName;

	private String platformShopId;

	private String testToken;

	private Long defaultWmsWarehouseId;

}
