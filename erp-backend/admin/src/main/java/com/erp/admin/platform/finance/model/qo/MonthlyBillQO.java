package com.erp.admin.platform.finance.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 月度账单查询对象。
 *
 * @author erp
 */
@Data
@Schema(title = "月度账单查询对象")
public class MonthlyBillQO {

    @Schema(title = "账期起 YYYY-MM")
    private String billMonthStart;

    @Schema(title = "账期止 YYYY-MM")
    private String billMonthEnd;

    @Schema(title = "WMS服务商(平台看全部，非空才过滤)")
    private Long wmsTenantId;

    @Schema(title = "状态集合 DRAFT/CONFIRMED/PAID/DISPUTED")
    private List<String> statuses;

}
