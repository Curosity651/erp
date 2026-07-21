package com.erp.admin.shop.model.dto;

import lombok.Data;

@Data
public class TestCredentialResponse {

	private String platform;

	private String shopName;

	private String platformShopId;

	private String testToken;

	/**
	 * 是否需要用户手动填写店铺名称
	 */
	private boolean shopNameRequired;

}
