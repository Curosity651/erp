package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_outbound_pick_task_order")
public class WmsOutboundPickTaskOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long outboundOrderId;
    private String outboundNo;
    private String toteNo;
    private Integer sortRequired;
    private String sortStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

