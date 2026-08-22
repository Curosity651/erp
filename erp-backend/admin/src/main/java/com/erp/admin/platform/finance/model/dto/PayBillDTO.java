package com.erp.admin.platform.finance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 应收账单付款确认参数。
 */
@Data
@Schema(title = "应收账单付款确认参数")
public class PayBillDTO {

    @NotNull(message = "请上传付款凭证")
    @Schema(title = "付款凭证文件ID", required = true)
    private Long paymentVoucherFileId;

}
