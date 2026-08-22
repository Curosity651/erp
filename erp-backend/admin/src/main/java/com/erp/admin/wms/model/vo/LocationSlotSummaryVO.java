package com.erp.admin.wms.model.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LocationSlotSummaryVO {

	private Long locationId;

	private Integer totalSlots = 0;

	private Integer occupiedSlots = 0;

	private Boolean blocked = false;

	private List<LocationSlotLevelSummaryVO> levels = new ArrayList<>();

}
