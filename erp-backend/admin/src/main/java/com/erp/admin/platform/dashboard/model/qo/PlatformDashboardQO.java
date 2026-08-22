package com.erp.admin.platform.dashboard.model.qo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 海外仓平台数据分析查询入参（跨全部货主聚合；仅当过滤集合非空时收窄）。
 *
 * @author erp
 */
@Data
@Schema(title = "平台数据分析查询入参")
public class PlatformDashboardQO {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "开始日期 YYYY-MM-DD")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "结束日期 YYYY-MM-DD")
    private LocalDate endDate;

    @Schema(title = "仓库过滤，空=全部")
    private List<Long> warehouseIds;

    @Schema(title = "WMS 服务商过滤，空=全部（按服务商收窄：其名下货主的业务数据）")
    private List<Long> wmsTenantIds;

    /** 服务端按莫斯科时区生成，客户端不可覆盖。 */
    @JsonIgnore
    @Schema(hidden = true)
    private LocalDate todayDate;

}
