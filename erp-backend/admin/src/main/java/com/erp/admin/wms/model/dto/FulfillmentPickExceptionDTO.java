package com.erp.admin.wms.model.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FulfillmentPickExceptionDTO {
	@NotBlank
	private String exceptionType;
	@NotBlank
	private String reason;
	private List<String> imageUrls;
}
