package com.erp.admin.wms.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OutboundPickReviewSummaryVO {
	private LocalDate workDate;
	private Long warehouseId;
	private Integer totalTaskCount;
	private Integer completedTaskCount;
	private Integer cancelledTaskCount;
	private Integer pendingTaskCount;
	private Integer pickingTaskCount;
	private Integer exceptionTaskCount;
	private Integer unprocessedTaskCount;
	private Boolean allProcessed;
	private Boolean reviewCurrent;
	private Long reviewId;
	private String reviewNo;
	private Long reviewedBy;
	private LocalDateTime reviewedTime;
	private String remark;
}
