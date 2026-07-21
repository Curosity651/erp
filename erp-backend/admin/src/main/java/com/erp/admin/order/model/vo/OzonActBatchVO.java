package com.erp.admin.order.model.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 一次【准备发运】产生的运单批次。
 * <p>
 * 所选订单可能跨店铺 / 跨物流方式，故一次操作可能生成多份运单。
 *
 * @author system
 */
@Data
@Schema(title = "Ozon运单批次")
public class OzonActBatchVO {

    @Schema(title = "批次号，前端据此轮询状态")
    private String batchNo;

    @Schema(title = "本批生成的运单列表")
    private List<OzonActVO> acts = new ArrayList<>();

    @Schema(title = "被排除的订单及原因")
    private List<OzonOrderRejectVO> failed = new ArrayList<>();
}
