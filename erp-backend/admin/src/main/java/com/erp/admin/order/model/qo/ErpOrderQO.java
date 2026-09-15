package com.erp.admin.order.model.qo;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表 查询对象
 *
 * @author erp 2025-09-27 23:16:08
 */
@Data
@Schema(title = "订单主表查询对象")
@ParameterObject
public class ErpOrderQO  {
	/**
	 * 订单主键
	 */
	@Parameter(description="订单主键")
	private Long id;

	/**
	 * 店铺ID (关联 shop.id)
	 */
	@Parameter(description="店铺ID")
	private Long shopId;

	@Parameter(description="平台: wildberries|ozon")
	private String platform;

	@Parameter(description="平台订单ID")
	private String platformOrderId;

	@Parameter(description="ERP 状态")
	private String erpStatus;

	@Parameter(description="货主业务状态")
	private String businessStatus;

	@Parameter(description="平台主状态")
	private String platformStatus;

	@Parameter(description="平台子状态")
	private String platformSubstatus;

	@Parameter(description="仓库ID")
	private String warehouseId;

	@Parameter(description="发货目的地仓库ID")
	private String destinationWarehouseId;

	@Parameter(description="履约类型 (FBS/FBO)")
	private String fulfillmentType;

	/**
	 * 仓型筛选（仅 Ozon FBS 订单有 warehouse_name）。
	 * BIG=大仓(仓库名含「大」字，需生成运单) / SMALL=小仓。
	 */
	@Parameter(description="仓型 BIG=大仓/SMALL=小仓")
	private String warehouseType;

	@Parameter(description="是否锁定 1/0")
	private Integer locked;

	@Parameter(description="关键字(平台单号/ERP单号/SKU)")
	private String keyword;

	/**
	 * SKU（按 ERP SKU 编码过滤，由 OrderQueryPreprocessor 预解析为 platformItemIds）
	 */
	@Parameter(description="ERP SKU 编码")
	private String skuCode;

	/**
	 * 预解析后的 platformItemId 列表（内部使用，由 OrderQueryPreprocessor 填充）
	 */
	private List<String> platformItemIds;

	// 时间范围
	@Parameter(description="创建时间开始")
	private LocalDateTime createTimeStart;
	@Parameter(description="创建时间结束")
	private LocalDateTime createTimeEnd;
	@Parameter(description="更新时间开始")
	private LocalDateTime updateTimeStart;
	@Parameter(description="更新时间结束")
	private LocalDateTime updateTimeEnd;
	@Parameter(description="同步时间开始")
	private LocalDateTime syncedAtStart;
	@Parameter(description="同步时间结束")
	private LocalDateTime syncedAtEnd;

	// 平台创建时间（订单生成时间 createdAt）
	@Parameter(description="平台创建时间开始(createdAt)")
	private LocalDateTime createdAtStart;
	@Parameter(description="平台创建时间结束(createdAt)")
	private LocalDateTime createdAtEnd;

}
