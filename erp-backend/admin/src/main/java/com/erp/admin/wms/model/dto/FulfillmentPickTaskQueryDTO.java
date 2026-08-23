package com.erp.admin.wms.model.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class FulfillmentPickTaskQueryDTO {
	private String taskNo;
	private Long warehouseId;
	private String taskStatus;
	private Long operatorId;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime startTime;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime endTime;
}
