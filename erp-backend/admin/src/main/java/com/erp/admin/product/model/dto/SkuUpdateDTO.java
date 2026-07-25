package com.erp.admin.product.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.erp.admin.product.validation.TaxRateValidatable;
import com.erp.admin.product.validation.ValidSkuCode;
import com.erp.admin.product.validation.ValidTaxRate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SKU更新DTO
 *
 * @author ballcat
 */
@Data
@Schema(title = "SKU更新DTO")
@ValidTaxRate
public class SkuUpdateDTO implements TaxRateValidatable {

	/**
	 * 主键ID
	 */
	@NotNull(message = "ID不能为空")
	@Schema(title = "主键ID", description = "更新时必填")
	private Long id;

	/**
	 * SKU编码
	 */
	@NotBlank(message = "SKU编码不能为空")
	@ValidSkuCode
	@Schema(title = "SKU编码", description = "SKU编码，必填且唯一")
	private String skuCode;

	/**
	 * SKU序号，唯一
	 */
	@NotNull(message = "SKU序号不能为空")
	@Min(value = 1, message = "SKU序号必须大于0")
	@Max(value = 999999, message = "SKU序号不能超过999999")
	@Schema(title = "SKU序号", description = "SKU序号，必填且唯一")
	private Integer skuNo;

	/**
	 * SPU编码
	 */
	@NotBlank(message = "SPU编码不能为空")
	@Size(max = 100, message = "SPU编码长度不能超过100个字符")
	@Schema(title = "SPU编码", description = "SPU编码，必填")
	private String spuCode;

	/**
	 * 销售国家
	 */
	@Size(max = 50, message = "销售国家长度不能超过50个字符")
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
	@Min(value = 1, message = "产品状态值不正确")
	@Max(value = 4, message = "产品状态值不正确")
	@Schema(title = "产品状态", description = "1:在售 2:停售 3:开发中 4:已下架")
	private Integer productStatus;

	/**
	 * 头程 1:陆运 2:空运
	 */
	@Min(value = 1, message = "头程类型值不正确")
	@Max(value = 2, message = "头程类型值不正确")
	@Schema(title = "头程", description = "1:陆运 2:空运")
	private Integer shippingType;

	/**
	 * 品牌编码
	 */
	@Size(max = 100, message = "品牌编码长度不能超过100个字符")
	@Schema(title = "品牌编码")
	private String brandCode;

	/**
	 * 项目组编码
	 */
	@Size(max = 100, message = "项目组编码长度不能超过100个字符")
	@Schema(title = "项目组编码")
	private String projectGroupCode;

	/**
	 * 产品描述
	 */
	@Schema(title = "产品描述")
	private String description;

	/**
	 * 中文名
	 */
	@Size(max = 200, message = "中文名长度不能超过200个字符")
	@Schema(title = "中文名")
	private String chineseName;

	/**
	 * 俄文名
	 */
	@Size(max = 200, message = "俄文名长度不能超过200个字符")
	@Schema(title = "俄文名")
	private String russianName;

	/**
	 * 包裹类型
	 */
	@Size(max = 100, message = "包裹类型长度不能超过100个字符")
	@Schema(title = "包裹类型")
	private String packageType;

	/**
	 * 计费重类型, 1=取大值，2=实重，3=体积重
	 */
	@Min(value = 1, message = "计费重类型值不正确")
	@Max(value = 3, message = "计费重类型值不正确")
	@Schema(title = "计费重类型", description = "1=取大值，2=实重，3=体积重")
	private Integer billingWeightType;

	/**
	 * 备注
	 */
	@Schema(title = "备注")
	private String remarks;

	/**
	 * 海关申报名
	 */
	@Size(max = 200, message = "海关申报名长度不能超过200个字符")
	@Schema(title = "海关申报名")
	private String customsDeclarationName;

	/**
	 * 海关申报代码
	 */
	@Size(max = 100, message = "海关申报代码长度不能超过100个字符")
	@Schema(title = "海关申报代码")
	private String customsDeclarationCode;

	/**
	 * 是否有排插
	 */
	@Schema(title = "是否有排插", description = "0:否 1:是")
	private Integer needsPower;

	/**
	 * 季节性产品，1=是，0=否
	 */
	@Schema(title = "季节性产品", description = "0:否 1:是")
	private Integer seasonal;

	/**
	 * BC纸箱最低耐破
	 */
	@Size(max = 100, message = "BC纸箱最低耐破长度不能超过100个字符")
	@Schema(title = "BC纸箱最低耐破")
	private String bcBoxMinBreakage;

	/**
	 * 分箱
	 */
	@Size(max = 200, message = "分箱长度不能超过200个字符")
	@Schema(title = "分箱")
	private String packaging;

	/**
	 * 表面颜色
	 */
	@Size(max = 100, message = "表面颜色长度不能超过100个字符")
	@Schema(title = "表面颜色")
	private String surfaceColor;

	/**
	 * 钢架颜色
	 */
	@Size(max = 100, message = "钢架颜色长度不能超过100个字符")
	@Schema(title = "钢架颜色")
	private String frameColor;

	/**
	 * 材质
	 */
	@Size(max = 200, message = "材质长度不能超过200个字符")
	@Schema(title = "材质")
	private String material;

	/**
	 * 是否有RGB灯带
	 */
	@Schema(title = "是否有RGB灯带", description = "0:否 1:是")
	private Integer hasRgbLight;

	/**
	 * 是否有玻璃
	 */
	@Schema(title = "是否有玻璃", description = "0:否 1:是")
	private Integer hasGlass;

	/**
	 * 重量
	 */
	@DecimalMin(value = "0.001", message = "重量必须大于0")
	@Digits(integer = 7, fraction = 3, message = "重量格式不正确")
	@Schema(title = "重量")
	private BigDecimal weight;

	/**
	 * 重量单位
	 */
	@Size(max = 10, message = "重量单位长度不能超过10个字符")
	@Schema(title = "重量单位")
	private String weightUnit;

	/**
	 * 包装长度
	 */
	@DecimalMin(value = "0.001", message = "包装长度必须大于0")
	@Digits(integer = 7, fraction = 3, message = "包装长度格式不正确")
	@Schema(title = "包装长度")
	private BigDecimal packageLength;

	/**
	 * 包装宽度
	 */
	@DecimalMin(value = "0.001", message = "包装宽度必须大于0")
	@Digits(integer = 7, fraction = 3, message = "包装宽度格式不正确")
	@Schema(title = "包装宽度")
	private BigDecimal packageWidth;

	/**
	 * 包装高度
	 */
	@DecimalMin(value = "0.001", message = "包装高度必须大于0")
	@Digits(integer = 7, fraction = 3, message = "包装高度格式不正确")
	@Schema(title = "包装高度")
	private BigDecimal packageHeight;

	/**
	 * 包装单位
	 */
	@Size(max = 10, message = "包装单位长度不能超过10个字符")
	@Schema(title = "包装单位")
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
	@Size(max = 100, message = "供应商编码长度不能超过100个字符")
	@Schema(title = "供应商编码")
	private String supplierCode;

	/**
	 * 是否含税
	 */
	@Schema(title = "是否含税", description = "0:否 1:是")
	private Integer includeTax;

	/**
	 * 税率
	 */
	@Schema(title = "税率", description = "含税时必填")
	private BigDecimal taxRate;

	/**
	 * 采购价
	 */
	@DecimalMin(value = "0.0", message = "采购价不能小于0")
	@Digits(integer = 10, fraction = 2, message = "采购价格式不正确")
	@Schema(title = "采购价")
	private BigDecimal purchasePrice;

	/**
	 * 起订量
	 */
	@Min(value = 1, message = "起订量必须大于0")
	@Schema(title = "起订量")
	private Integer minimumOrderQuantity;

	/**
	 * 生产周期(天)
	 */
	@Min(value = 1, message = "生产周期必须大于0")
	@Schema(title = "生产周期", description = "单位：天")
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
	 * SKU文件映射表（更新时的完整文件映射）
	 */
	@Schema(title = "SKU文件映射表", description = "key为文件类型，value为该类型的文件列表，传入完整映射用于全量更新")
	private Map<String, List<SkuFileDTO>> files;

	@Size(max = 20, message = "一个SKU最多维护20个商品条码")
	@Schema(title = "商品条码列表", description = "支持一个ERP SKU对应多个EAN/UPC或内部商品条码")
	private List<String> barcodes;

}
