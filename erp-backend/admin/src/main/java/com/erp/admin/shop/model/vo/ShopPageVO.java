package com.erp.admin.shop.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 店铺（含凭证信息）分页视图对象
 *
 * @author erp 2025-09-21 21:11:47
 */
@Data
@Schema(title = "店铺（含凭证信息）分页视图对象")
public class ShopPageVO {

	/**
	 * 店铺主键
	 */
	@Schema(title = "店铺主键")
	private Long id;

	/**
	 * 平台: wildberries|ozon
	 */
	@Schema(title = "平台: wildberries|ozon")
	private String platform;

	/**
	 * 平台侧店铺 id/code (可为空)
	 */
	@Schema(title = "平台侧店铺 id/code (可为空)")
	private String platformShopId;

	/**
	 * 店铺名称
	 */
	@Schema(title = "店铺名称")
	private String name;

	@Schema(title = "Erp显示的店铺名称")
	private String erpShopName;

	/**
	 * 1 启用 / 0 禁用
	 */
	@Schema(title = "1 启用 / 0 禁用")
	private Integer status;

	/**
	 * 凭证信息，wildberries 存 { "api_key": "xxx" }，ozon 存 { "client_id": "xxx", "api_key":
	 * "xxx" }
	 */
	@Schema(title = "凭证信息，wildberries 存 { \"api_key\": \"xxx\" }，ozon 存 { \"client_id\": \"xxx\", \"api_key\": \"xxx\" }")
	private String credential;

	/**
	 * 上次凭证测试时间
	 */
	@Schema(title = "上次凭证测试时间")
	private LocalDateTime lastTestedAt;

	/**
	 * 创建人
	 */
	@Schema(title = "创建人")
	private Long createdBy;

	/**
	 * 创建时间
	 */
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	/**
	 * 最近一次测试状态：0 未测试 / 1 成功 / 2 失败
	 */
	@Schema(title = "最近一次测试状态：0 未测试 / 1 成功 / 2 失败")
	private Integer lastTestStatus;

}