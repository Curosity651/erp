package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 退货单（列表 + 详情）。
 *
 * @author erp
 */
@Data
@Schema(title = "退货单")
public class ReturnOrderVO {

    private Long id;

    private String returnNo;

    private String returnBatchNo;

    private Long erpTenantId;

    private String ownerName;

    @Schema(title = "所属WMS服务商ID")
    private Long operatorId;

    @Schema(title = "所属WMS服务商名称")
    private String operatorName;

    private Long warehouseId;

    private String warehouseName;

    private Integer skuKinds;

    private Integer totalQty;

    @Schema(title = "状态 PENDING_OWNER/PENDING_OPERATION/COMPLETED/CLOSED")
    private String status;

    private String createTime;

    private Long receivedBy;

    private String receivedByName;

    private String receivedTime;

    private Long qcBy;

    private String qcByName;

    private String qcTime;

    private Long closedBy;

    private String closedByName;

    private String closedTime;

    private Long dispositionBy;

    private String dispositionByName;

    private String dispositionTime;

    private Long processedBy;

    private String processedByName;

    private String processedTime;

    @Schema(title = "明细(详情才带)")
    private List<ReturnOrderItemVO> items;

}
