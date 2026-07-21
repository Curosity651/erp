package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存操作单查询条件
 *
 * @author erp
 */
@Data
@Schema(title = "库存操作单查询条件")
public class StockIOOrderQO {

    @Schema(title = "操作单号")
    private String ioNo;

    @Schema(title = "仓库ID")
    private Long warehouseId;

    @Schema(title = "操作类型")
    private String ioType;

    @Schema(title = "来源单据号")
    private String sourceNo;

    @Schema(title = "单据状态")
    private String orderStatus;

    @Schema(title = "操作时间开始")
    private LocalDateTime ioTimeStart;

    @Schema(title = "操作时间结束")
    private LocalDateTime ioTimeEnd;

}
