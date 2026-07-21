package com.erp.admin.shop.model.vo;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class ShopDetailVO {

	private Long id;

	private String platform;

	private String name;

	private String erpShopName;

	private String platformShopId;

	private Integer status;

	private Integer lastTestStatus; // 0 未测试 1 成功 2 失败

	private LocalDateTime lastTestedAt;

	private Map<String, String> credentialMask;

}
