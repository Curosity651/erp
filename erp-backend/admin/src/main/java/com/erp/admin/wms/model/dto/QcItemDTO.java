package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 质检数据项DTO
 * <p>
 * 用于采购单编辑时提交单个SKU的质检数据
 *
 * @author erp
 */
@Data
@Schema(title = "质检数据项DTO")
public class QcItemDTO {

	@NotBlank(message = "SKU编码不能为空")
	@Size(max = 100, message = "SKU编码长度不能超过100个字符")
	@Schema(title = "SKU编码")
	private String skuCode;

	@Positive(message = "包装长度必须大于0")
	@Schema(title = "包装长度(cm)")
	private BigDecimal lengthCm;

	@Positive(message = "包装宽度必须大于0")
	@Schema(title = "包装宽度(cm)")
	private BigDecimal widthCm;

	@Positive(message = "包装高度必须大于0")
	@Schema(title = "包装高度(cm)")
	private BigDecimal heightCm;

	@Positive(message = "毛重必须大于0")
	@Schema(title = "毛重(KG)")
	private BigDecimal grossWeightKg;

	@Positive(message = "净重必须大于0")
	@Schema(title = "净重(KG)")
	private BigDecimal netWeightKg;

	@Schema(title = "质检报告文件ID（关联sys_file表）")
	private Long qcFileId;

}
