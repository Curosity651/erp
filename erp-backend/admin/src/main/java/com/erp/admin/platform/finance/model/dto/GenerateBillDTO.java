package com.erp.admin.platform.finance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 生成/重算账单入参。
 *
 * @author erp
 */
@Data
@Schema(title = "生成账单入参")
public class GenerateBillDTO {

    @NotBlank(message = "账期不能为空")
    @Schema(title = "账期 YYYY-MM")
    private String billMonth;

    @Schema(title = "WMS服务商(不传=当月全部服务商)")
    private Long wmsTenantId;

}
