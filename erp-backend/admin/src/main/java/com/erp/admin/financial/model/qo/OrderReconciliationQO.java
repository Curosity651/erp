package com.erp.admin.financial.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单财务对账查询对象
 *
 * @author system
 */
@Data
@ParameterObject
@Schema(description = "订单财务对账查询对象")
public class OrderReconciliationQO {

    @Schema(description = "店铺ID列表")
    private List<Long> shopIds;

    @NotNull(message = "报表类型不能为空")
    @Schema(description = "报表类型：weekly-周报, daily-日报", requiredMode = Schema.RequiredMode.REQUIRED, example = "weekly")
    private String periodType;

    @Schema(description = "对账状态列表：MATCHED-已对账, PENDING-待履约, IN_TRANSIT-运输中, CANCELED-已取消, ANOMALY-异常")
    private List<String> reconciliationStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "订单开始日期")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "订单结束日期")
    private LocalDate endDate;

    @Schema(description = "关键词（订单号/RID/商品编码）")
    private String keyword;
}
