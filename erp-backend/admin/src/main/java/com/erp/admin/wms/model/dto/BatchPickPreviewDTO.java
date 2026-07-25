package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(title = "批量拣货任务拆分预览")
public class BatchPickPreviewDTO {

    @NotEmpty(message = "请选择待下架订单")
    private List<Long> outboundOrderIds;

    @Min(value = 1, message = "单波次订单数不能小于1")
    @Max(value = 50, message = "单波次订单数不能超过50")
    private Integer maxOrdersPerTask = 20;

    private Boolean wholePalletPriority = Boolean.TRUE;
}

