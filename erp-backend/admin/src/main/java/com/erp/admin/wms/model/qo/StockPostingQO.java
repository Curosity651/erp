package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存过账单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "库存过账单查询对象")
public class StockPostingQO {

    @Schema(title = "过账单号")
    private String postingNo;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "过账类型")
    private String postingType;

    @Schema(title = "来源单据类型")
    private String sourceType;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "过账时间开始")
    private LocalDateTime postTimeStart;

    @Schema(title = "过账时间结束")
    private LocalDateTime postTimeEnd;

    /** 货主作用域（服务端按身份注入，忽略前端传入）：null=平台看全部；非空=IN 过滤。 */
    @Schema(hidden = true)
    private java.util.List<Long> erpTenantIds;

}
