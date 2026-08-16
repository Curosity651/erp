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
 * 采购入库单实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_inbound_order")
@Schema(title = "采购入库单实体")
public class PurchaseInboundOrder {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "入库单号")
    private String inboundNo;

    @Schema(title = "来源: PURCHASE采购 / MANUAL自定义 / CUSTOM_RETURN自定义退货")
    private String sourceType;

    @Schema(title = "退货类型(仅自定义退货): PLATFORM_BATCH平台批量退货 / NO_ORDER无单退件 / SAMPLE_BACK样品收回 / WRONG_SHIPMENT发错召回 / OTHER其他")
    private String returnType;

    @Schema(title = "关联单号(仅自定义退货, 纯文本参考: 平台订单号/物流追踪号等)")
    private String refNo;

    @Schema(title = "货物归属(货主)租户ID")
    private Long erpTenantId;

    @Schema(title = "关联物流单ID(自定义入库为空)")
    private Long shippingOrderId;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "入库日期")
    private LocalDate inboundDate;

    @Schema(title = "单据状态: DRAFT草稿 / SUBMITTED已提交 / RECEIVED已收货 / COMPLETED已完成 / CANCELLED已取消")
    private String orderStatus;

    @Schema(title = "关联库存过账单ID（确认入库时生成）")
    private Long stockPostingId;

    @Schema(title = "收货操作员ID")
    private Long receiveBy;

    @Schema(title = "收货时间")
    private LocalDateTime receiveTime;

    @Schema(title = "收货现场照片文件ID，逗号分隔")
    private String receiveEvidenceFileIds;

    @Schema(title = "上架操作员ID")
    private Long putawayBy;

    @Schema(title = "上架时间")
    private LocalDateTime putawayTime;

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
