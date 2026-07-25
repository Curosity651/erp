package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_outbound_pick_task_line")
public class WmsOutboundPickTaskLine {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long physicalInventoryId;
    private Long palletId;
    private String palletNo;
    private String slotCode;
    private String locationCode;
    private String skuCode;
    private LocalDate inboundDate;
    private Integer pickOrder;
    private Integer plannedQty;
    private Integer pickedQty;
    private Integer shortageQty;
    private String exceptionReason;
    private String pickStrategy;
    private String lineStatus;
    @Version
    private Integer version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
