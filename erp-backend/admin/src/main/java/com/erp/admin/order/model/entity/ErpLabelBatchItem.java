package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("erp_label_batch_item")
@Schema(title = "面单打印批次-订单映射")
public class ErpLabelBatchItem {

    @TableId
    private Long id;

    private Long batchId;

    private Long shopId;

    private Long orderId;

    private String platformOrderId;

    private String supplyId;

    private String warehouseId;

    private String platform;

    private String erpSkuCode;

    private Integer skuQty;

    @Schema(title = "分组键: 仓库+SKU")
    private String groupKey;

    @Schema(title = "订单项状态: PENDING|SUCCESS|FAILED")
    private String status;

    @Schema(title = "失败码")
    private String failCode;

    @Schema(title = "失败原因")
    private String errorMsg;

    @Schema(title = "所属订单PDF文件ID")
    private Long orderFileId;

    @Schema(title = "所属批次PDF文件ID")
    private Long supplyFileId;
}


