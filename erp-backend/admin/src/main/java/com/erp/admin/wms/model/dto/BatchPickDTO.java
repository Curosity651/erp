package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(title = "批量创建拣货任务")
public class BatchPickDTO {

    @NotEmpty(message = "请选择待下架订单")
    private List<Long> outboundOrderIds;

    @NotNull(message = "拣货员不能为空")
    private Long pickerId;

    @Min(value = 1, message = "单波次订单数不能小于1")
    @Max(value = 50, message = "单波次订单数不能超过50")
    private Integer maxOrdersPerTask = 20;

    private Boolean wholePalletPriority = Boolean.TRUE;
}

