package com.erp.admin.platform.finance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标记争议入参。
 *
 * @author erp
 */
@Data
@Schema(title = "标记争议入参")
public class DisputeBillDTO {

    @Schema(title = "争议原因")
    private String remark;

}
