package com.erp.admin.wms.model.vo;

import com.erp.admin.wms.model.enums.ForecastStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 库存预测汇总结果
 *
 * @author erp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "库存预测汇总结果")
public class ForecastSummaryResult {

    @Schema(title = "总数")
    private int total;

    @Schema(title = "各状态数量")
    private Map<ForecastStatus, Integer> statusCounts;

    @Schema(title = "预警阈值天数")
    private int thresholdDays;

    @Schema(title = "数据列表")
    private List<ForecastSummaryVO> list;

    public static ForecastSummaryResult empty(int thresholdDays) {
        Map<ForecastStatus, Integer> counts = new EnumMap<>(ForecastStatus.class);
        Arrays.stream(ForecastStatus.values()).forEach(s -> counts.put(s, 0));
        return new ForecastSummaryResult(0, counts, thresholdDays, Collections.emptyList());
    }
}
