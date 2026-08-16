package com.erp.admin.shop.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 店铺（含凭证信息）
 *
 * @author erp 2025-09-21 21:11:47
 */
@Data
@TableName("shop")
@Schema(title = "店铺（含凭证信息）")
public class Shop {

	/**
	 * 店铺主键
	 */
	@TableId
	@Schema(title = "店铺主键")
	private Long id;

	/**
	 * 所属租户（货主）。只读：写入由多租户拦截器自动注入，此处仅供后台同步任务读取以 TenantContext.runAs 建立上下文。
	 * insert/update 用 NEVER 不参与，避免覆盖拦截器注入。
	 */
	@TableField(value = "tenant_id", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
	@Schema(title = "所属租户(只读)")
	private Long tenantId;

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

	@Schema(title = "默认内部WMS仓库ID")
	private Long defaultWmsWarehouseId;

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
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
