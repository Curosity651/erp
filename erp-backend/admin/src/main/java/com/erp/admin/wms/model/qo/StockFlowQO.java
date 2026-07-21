package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存流水查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "库存流水查询条件")
public class StockFlowQO {

    @Schema(title = "区域ID")
    private Long regionId;

    @Schema(title = "区域内仓库ID列表（内部使用，由 Service 层填充）", hidden = true)
    private List<Long> regionWarehouseIds;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "库存桶: AVAILABLE/RESERVED/IN_TRANSIT")
    private String bucket;

    @Schema(title = "过账类型")
    private String postingType;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "过账单号")
    private String postingNo;

    @Schema(title = "开始时间")
    private LocalDateTime startTime;

    @Schema(title = "结束时间")
    private LocalDateTime endTime;

    @Schema(title = "过账类型列表（支持多选）")
    private List<String> postingTypes;

    @Schema(title = "方向: IN/OUT")
    private String direction;

    /** 货主作用域（服务端按身份注入，忽略前端传入）：null=平台看全部；非空=IN 过滤。 */
    @Schema(hidden = true)
    private List<Long> erpTenantIds;

}
