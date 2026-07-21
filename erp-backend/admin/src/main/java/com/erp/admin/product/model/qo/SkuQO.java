package com.erp.admin.product.model.qo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU管理表 查询对象
 *
 * @author ballcat 2025-07-27 02:02:06
 */
@Data
@Schema(title = "SKU查询对象")
public class SkuQO {

	/**
	 * SKU编码
	 */
	@Schema(title = "SKU编码")
	private String skuCode;

	/**
	 * SKU序号
	 */
	@Schema(title = "SKU序号")
	private Integer skuNo;

	/**
	 * SPU编码
	 */
	@Schema(title = "SPU编码")
	private String spuCode;

	/**
	 * 销售国家
	 */
	@Schema(title = "销售国家")
	private String salesCountry;

	/**
	 * 品类ID
	 */
	@Schema(title = "品类ID")
	private Long categoryId;

	/**
	 * 产品状态
	 */
	@Schema(title = "产品状态 1:在售 2:停售 3:开发中 4:已下架")
	private Integer productStatus;

	/**
	 * 头程类型
	 */
	@Schema(title = "头程 1:陆运 2:空运")
	private Integer shippingType;

	/**
	 * 品牌编码
	 */
	@Schema(title = "品牌编码")
	private String brandCode;

	/**
	 * 项目组编码
	 */
	@Schema(title = "项目组编码")
	private String projectGroupCode;

	/**
	 * SKU名称（中文名或俄文名）
	 */
	@Schema(title = "SKU名称")
	private String skuName;

	/**
	 * 供应商编码
	 */
	@Schema(title = "供应商编码")
	private String supplierCode;

	/**
	 * 创建时间开始
	 */
	@Schema(title = "创建时间开始")
	private LocalDateTime createTimeStart;

	/**
	 * 创建时间结束
	 */
	@Schema(title = "创建时间结束")
	private LocalDateTime createTimeEnd;

	/**
	 * 包裹类型
	 */
	@Schema(title = "包裹类型")
	private String packageType;

	/**
	 * 表面颜色
	 */
	@Schema(title = "表面颜色")
	private String surfaceColor;

	/**
	 * 钢架颜色
	 */
	@Schema(title = "钢架颜色")
	private String frameColor;

	/**
	 * 材质
	 */
	@Schema(title = "材质")
	private String material;

	/**
	 * 是否有RGB灯带
	 */
	@Schema(title = "是否有RGB灯带 0:否 1:是")
	private Integer hasRgbLight;

	/**
	 * 是否有玻璃
	 */
	@Schema(title = "是否有玻璃 0:否 1:是")
	private Integer hasGlass;

	/**
	 * 是否有排插
	 */
	@Schema(title = "是否有排插 0:否 1:是")
	private Integer needsPower;

	/**
	 * 是否季节性产品
	 */
	@Schema(title = "是否季节性产品 0:否 1:是")
	private Integer seasonal;

	/**
	 * 计费重类型
	 */
	@Schema(title = "计费重类型 1:取大值 2:实重 3:体积重")
	private Integer billingWeightType;

	/**
	 * 开发人员ID
	 */
	@Schema(title = "开发人员ID")
	private Long developerId;

	/**
	 * 运营人员ID
	 */
	@Schema(title = "运营人员ID")
	private Long operatorId;

	/**
	 * 质检人员ID
	 */
	@Schema(title = "质检人员ID")
	private Long qualityInspectorId;

	/**
	 * 采购人员ID
	 */
	@Schema(title = "采购人员ID")
	private Long purchaserId;

}
