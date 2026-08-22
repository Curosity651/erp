package com.erp.admin.platform.finance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("wms_service_contract")
public class WmsServiceContract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private Long wmsTenantId;
    private Long warehouseId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer rackUnitCount;
    private BigDecimal monthlyRentPerUnit;
    private BigDecimal warehouseDeposit;
    private BigDecimal subscriptionTotal;
    private BigDecimal refundableRate;
    private BigDecimal refundableAmount;
    private BigDecimal serviceAmount;
    private BigDecimal monthlyServiceRecognition;
    private String contractFileUrl;
    private Long contractFileId;
    private String contractStatus;
    private String paymentStatus;
    private LocalDateTime receivedTime;
    private LocalDateTime settledTime;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
