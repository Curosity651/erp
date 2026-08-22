package com.erp.admin.wms.model.qo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PackShipPackageQO {

	private String keyword;
	private String platform;
	private String workStatus;
	private Long erpTenantId;
	private Long wmsTenantId;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate createTimeStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate createTimeEnd;

}
