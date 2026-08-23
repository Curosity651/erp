package com.erp.admin.wms.model.vo;

import java.util.List;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import lombok.Data;

@Data
public class FulfillmentPickTaskOrderVO {
	private WmsFulfillmentPickTaskOrder taskOrder;
	private WmsFulfillmentOrder fulfillmentOrder;
	private List<WmsFulfillmentPickTaskLine> routeLines;
	private String firstLocationCode;
	private Integer skuCount;
	private Integer totalQuantity;
	private Integer pickedQuantity;
}
