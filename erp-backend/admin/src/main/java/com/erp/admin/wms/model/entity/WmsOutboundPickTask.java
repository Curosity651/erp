package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_outbound_pick_task")
public class WmsOutboundPickTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long platformTenantId;
    private Long warehouseId;
    private Long erpTenantId;
    private String sourceType;
    private String taskType;
    private String taskStatus;
    private Long pickerId;
    private String pickerName;
    /** 任务关联的销售/自定义出库单数量，不是电商平台订单数量。 */
    private Integer orderCount;
    /** 销售出库单下关联的电商平台订单数量；自定义出库为 0。 */
    private Integer salesOrderCount;
    private Integer skuCount;
    private Integer totalQuantity;
    private Integer wholePalletCount;
    private Integer secondaryOrderCount;
    private Long exceptionLineId;
    private String exceptionReason;
    private LocalDateTime exceptionTime;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
