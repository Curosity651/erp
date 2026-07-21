package com.erp.admin.product.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.admin.product.validation.TaxRateValidatable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU管理表
 *
 * @author ballcat 2025-07-30 01:12:42
 */
@Data
@TableName("sku")
@Schema(title = "SKU管理表")
public class Sku implements TaxRateValidatable {

	/**
	 * 主键ID
	 */
	@TableId
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * SKU编码
	 */
	@Schema(title = "SKU编码")
	private String skuCode;

	/**
	 * SKU序号，唯一
	 */
	@Schema(title = "SKU序号，唯一")
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
	 * 产品状态 1:在售 2:停售 3:开发中 4:已下架
	 */
	@Schema(title = "产品状态 1:在售 2:停售 3:开发中 4:已下架")
	private Integer productStatus;

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
	 * 中文名
	 */
	@Schema(title = "中文名")
	private String chineseName;

	/**
	 * 俄文名
	 */
	@Schema(title = "俄文名")
	private String russianName;

	/**
	 * 产品描述
	 */
	@Schema(title = "产品描述")
	private String description;

	/**
	 * 备注
	 */
	@Schema(title = "备注")
	private String remarks;

	/**
	 * 海关申报名
	 */
	@Schema(title = "海关申报名")
	private String customsDeclarationName;

	/**
	 * 海关申报代码
	 */
	@Schema(title = "海关申报代码")
	private String customsDeclarationCode;

	/**
	 * 是否有排插
	 */
	@Schema(title = "是否有排插")
	private Integer needsPower;

	/**
	 * 季节性产品，1=是，0=否
	 */
	@Schema(title = "季节性产品，1=是，0=否")
	private Integer seasonal;

	/**
	 * 头程 1:陆运 2:空运
	 */
	@Schema(title = "头程 1:陆运 2:空运")
	private Integer shippingType;

	/**
	 * 包裹类型, normal=普通，magnetic=含磁
	 */
	@Schema(title = "包裹类型, normal=普通，magnetic=含磁")
	private String packageType;

	/**
	 * 计费重类型, 1=取大值，2=实重，3=体积重
	 */
	@Schema(title = "计费重类型, 1=取大值，2=实重，3=体积重")
	private Integer billingWeightType;

	/**
	 * BC纸箱最低耐破
	 */
	@Schema(title = "BC纸箱最低耐破")
	private String bcBoxMinBreakage;

	/**
	 * 分箱
	 */
	@Schema(title = "分箱")
	private String packaging;

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
	 * 是否有RGB灯带，1=是，0=否
	 */
	@Schema(title = "是否有RGB灯带")
	private Integer hasRgbLight;

	/**
	 * 是否有玻璃，1=是，0=否
	 */
	@Schema(title = "是否有玻璃")
	private Integer hasGlass;

	/**
	 * 重量
	 */
	@Schema(title = "重量")
	private BigDecimal weight;

	/**
	 * 重量单位
	 */
	@Schema(title = "重量单位")
	private String weightUnit;

	/**
	 * 包装长度
	 */
	@Schema(title = "包装长度")
	private BigDecimal packageLength;

	/**
	 * 包装宽度
	 */
	@Schema(title = "包装宽度")
	private BigDecimal packageWidth;

	/**
	 * 包装高度
	 */
	@Schema(title = "包装高度")
	private BigDecimal packageHeight;

	/**
	 * 包装尺寸单位
	 */
	@Schema(title = "包装尺寸单位")
	private String packageUnit;

	/**
	 * 每托数量（件）.
	 */
	@Schema(title = "每托数量（件）")
	private Integer quantityPerPallet;

	/**
	 * 功能性能要求
	 */
	@Schema(title = "功能性能要求")
	private String functionalRequirements;

	/**
	 * 供应商编码
	 */
	@Schema(title = "供应商编码")
	private String supplierCode;

	/**
	 * 是否含税
	 */
	@Schema(title = "是否含税")
	private Integer includeTax;

	/**
	 * 税率
	 */
	@Schema(title = "税率")
	private BigDecimal taxRate;

	/**
	 * 采购价
	 */
	@Schema(title = "采购价")
	private BigDecimal purchasePrice;

	/**
	 * 起订量
	 */
	@Schema(title = "起订量")
	private Integer minimumOrderQuantity;

	/**
	 * 生产周期(天)
	 */
	@Schema(title = "生产周期(天)")
	private Integer productionCycle;

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
	private Long qcId;

	/**
	 * 采购人员ID
	 */
	@Schema(title = "采购人员ID")
	private Long purchaserId;

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

	/**
	 * 创建人
	 */
	@Schema(title = "创建人")
	private String createBy;

	/**
	 * 更新人
	 */
	@Schema(title = "更新人")
	private String updateBy;

}
