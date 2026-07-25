package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wms_contract_rack")
public class WmsContractRack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long rackAssignmentId;
    private String rackNo;
    private LocalDateTime createTime;
}

