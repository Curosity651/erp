package com.erp.admin.order.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单份拣货单（每个店铺一份）。
 *
 * @author system
 */
@Data
@Schema(title = "Ozon拣货单文件")
public class OzonPickListFileVO {

    private Long shopId;

    private String shopName;

    @Schema(title = "纳入的订单数")
    private Integer orderCount;

    @Schema(title = "涉及的 SKU 去重数")
    private Integer skuCount;

    private String fileName;

    private String objectKey;

    @Schema(title = "下载地址")
    private String downloadUrl;

    @Schema(title = "生成失败时的原因")
    private String errorMsg;
}
