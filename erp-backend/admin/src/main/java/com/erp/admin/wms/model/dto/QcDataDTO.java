package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 质检数据DTO
 * <p>
 * 用于采购单编辑时提交各SKU的质检数据
 *
 * @author erp
 */
@Data
@Schema(title = "质检数据DTO")
public class QcDataDTO {

	@Valid
	@Schema(title = "各SKU的质检数据列表")
	private List<QcItemDTO> items;

}
