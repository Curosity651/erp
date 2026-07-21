package com.erp.admin.platform.finance.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 生成/重算账单结果。
 *
 * @author erp
 */
@Data
@Schema(title = "生成账单结果")
public class GenerateBillResultVO {

    @Schema(title = "新建账单数")
    private int created;

    @Schema(title = "重算覆盖(原草稿)数")
    private int recalculated;

    @Schema(title = "已确认/已付款跳过数")
    private int skipped;

}
