package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class FulfillmentHandoverDTO {

	@NotBlank(message = "车牌不能为空")
	@Size(max = 64, message = "车牌不能超过64个字符")
	private String vehiclePlate;

	@NotBlank(message = "司机不能为空")
	@Size(max = 128, message = "司机不能超过128个字符")
	private String driverName;

	@Size(max = 64, message = "司机电话不能超过64个字符")
	private String driverPhone;

	@NotNull(message = "发车时间不能为空")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime departureTime;

	@NotBlank(message = "目的地不能为空")
	@Size(max = 1000, message = "目的地不能超过1000个字符")
	private String destination;

	@NotNull(message = "运费不能为空")
	@DecimalMin(value = "0", message = "运费不能小于0")
	private BigDecimal freightCost;

	@Size(max = 8, message = "币种不能超过8个字符")
	private String currency;

	@Size(max = 500, message = "备注不能超过500个字符")
	private String remark;

	@Size(max = 6, message = "物流照片最多上传6张")
	private List<Long> photoFileIds;
}
