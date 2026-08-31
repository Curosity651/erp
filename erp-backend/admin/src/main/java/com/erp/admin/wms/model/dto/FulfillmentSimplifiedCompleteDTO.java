package com.erp.admin.wms.model.dto;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

@Data
public class FulfillmentSimplifiedCompleteDTO {
	@NotEmpty(message = "请至少上传一张作业凭证")
	private List<Long> evidenceFileIds;
}
