package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class BatchPickResultVO {
    private Integer taskCount;
    /** 创建任务包含的出库单数。 */
    private Integer orderCount;
    private Integer salesOrderCount;
    private List<Long> taskIds;
    private List<String> taskNos;
}
