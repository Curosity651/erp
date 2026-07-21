package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 付款信息DTO
 * <p>
 * 用于采购单编辑时提交付款状态和凭证信息
 *
 * @author erp
 */
@Data
@Schema(title = "付款信息DTO")
public class PaymentInfoDTO {

	@Schema(title = "首付款状态: 0-未付 / 1-已付")
	private Integer prepayStatus;

	@Schema(title = "首付款凭证文件ID（状态为已付时必填）")
	private Long prepayVoucherFileId;

	@Schema(title = "尾款状态: 0-未付 / 1-已付")
	private Integer balanceStatus;

	@Schema(title = "尾款凭证文件ID（状态为已付时必填）")
	private Long balanceVoucherFileId;

}
