package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业用 SKU 速查结果（D 数据级可见性 ②）。
 *
 * <p>平台/服务商作业时按 SKU 编码识别商品（不开商品浏览页）。携带 erpTenantId 以便上架时写对货主。
 *
 * @author erp
 */
@Data
@Schema(title = "作业用SKU速查")
public class SkuLookupVO {

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "货物归属(货主) tenant_id")
	private Long erpTenantId;

	@Schema(title = "中文名")
	private String chineseName;

	@Schema(title = "俄文名")
	private String russianName;

	@Schema(title = "单重")
	private BigDecimal weight;

	private String weightUnit;

	@Schema(title = "包装长")
	private BigDecimal packageLength;

	@Schema(title = "包装宽")
	private BigDecimal packageWidth;

	@Schema(title = "包装高")
	private BigDecimal packageHeight;

	private String packageUnit;

	private Integer quantityPerPallet;

}
