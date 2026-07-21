package com.erp.admin.order.model.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 一次【打印拣货单】产生的批次，按店铺各出一份。
 *
 * @author system
 */
@Data
@Schema(title = "Ozon拣货单批次")
public class OzonPickListBatchVO {

    @Schema(title = "批次号（同时作为 OSS 目录名）")
    private String batchNo;

    @Schema(title = "生成的拣货单，每个店铺一份")
    private List<OzonPickListFileVO> files = new ArrayList<>();

    @Schema(title = "被排除的订单及原因")
    private List<OzonOrderRejectVO> failed = new ArrayList<>();
}
