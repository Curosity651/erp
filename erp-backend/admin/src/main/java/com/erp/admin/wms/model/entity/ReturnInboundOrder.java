package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 退货入库单实体
 *
 * @author erp
 */
@Data
@TableName("wms_return_inbound_order")
@Schema(title = "退货入库单实体")
public class ReturnInboundOrder {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
    private Long erpTenantId;

    @Schema(title = "退货单号")
    private String returnNo;

    @Schema(title = "海外仓退货批次号")
    private String returnBatchNo;

    @Schema(title = "ERP订单ID")
    private Long erpOrderId;

    @Schema(title = "被退的订单明细ID(平台质检完成时回写已退数量用)")
    private Long orderItemId;

    @Schema(title = "平台订单号(冗余)")
    private String platformOrderId;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "退货日期")
    private LocalDate returnDate;

    @Schema(title = "退货原因: NOT_WANTED/DAMAGED/WRONG_ITEM/QUALITY_ISSUE/OTHER")
    private String returnReason;

    @Schema(title = "退货总数量")
    private Integer totalQuantity;

    @Schema(title = "合格入库总数量")
    private Integer qualifiedQuantity;

    @Schema(title = "不合格总数量")
    private Integer unqualifiedQuantity;

    @Schema(title = "转残品数量")
    private Integer toDamagedQuantity;

    @Schema(title = "直接报废数量")
    private Integer scrapQuantity;

    @Schema(title = "可退货数量(快照)")
    private Integer returnableQuantity;

    @Schema(title = "退货质检状态 RETURN_PENDING/QC_PENDING/COMPLETED（平台质检工作流）")
    private String returnStatus;

    private Long receivedBy;

    private LocalDateTime receivedTime;

    private Long dispositionBy;

    private LocalDateTime dispositionTime;

    private Long processedBy;

    private LocalDateTime processedTime;

    private Long qcBy;

    private LocalDateTime qcTime;

    private Long closedBy;

    private LocalDateTime closedTime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(title = "逻辑删除标记")
    private Long deleted;

}
