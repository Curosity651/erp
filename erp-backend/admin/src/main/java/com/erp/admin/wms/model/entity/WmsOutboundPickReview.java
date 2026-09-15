package com.erp.admin.wms.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_outbound_pick_review")
public class WmsOutboundPickReview {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long tenantId;
	private String reviewNo;
	private LocalDate workDate;
	private Long warehouseId;
	private String reviewStatus;
	private Integer taskCount;
	private Integer completedCount;
	private Integer cancelledCount;
	private Integer exceptionCount;
	private LocalDateTime taskSnapshotTime;
	private Long reviewedBy;
	private LocalDateTime reviewedTime;
	private String remark;
	private Integer version;
	private Long createBy;
	private LocalDateTime createTime;
	private Long updateBy;
	private LocalDateTime updateTime;
	@TableLogic
	private Long deleted;
}
