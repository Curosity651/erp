package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 物流渠道选项。
 *
 * @author erp
 */
@Data
@Schema(title = "物流渠道选项")
public class LogisticsChannelVO {

    @Schema(title = "渠道代码")
    private String code;

    @Schema(title = "渠道名称")
    private String name;

}
