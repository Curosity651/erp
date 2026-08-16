package com.erp.admin.wms.model.vo;

import java.util.List;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import lombok.Data;

@Data
public class FulfillmentPickTaskDetailVO {
	private WmsFulfillmentPickTask task;
	private List<WmsFulfillmentPickTaskOrder> orders;
	private List<WmsFulfillmentPickTaskLine> lines;
}
