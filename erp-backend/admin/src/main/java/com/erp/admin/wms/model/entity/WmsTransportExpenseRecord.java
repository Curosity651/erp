package com.erp.admin.wms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_transport_expense_record")
public class WmsTransportExpenseRecord {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long tenantId;
	private String expenseType;
	private LocalDate expenseDate;
	private String settlementMonth;
	private String driverName;
	private BigDecimal amount;
	private String currency;
	private String note;
	private Long createBy;
	private LocalDateTime createTime;
	private Long updateBy;
	private LocalDateTime updateTime;
	@TableLogic
	private Long deleted;
}
