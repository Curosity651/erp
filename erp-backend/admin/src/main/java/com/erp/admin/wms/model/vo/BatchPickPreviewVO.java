package com.erp.admin.wms.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class BatchPickPreviewVO {
    private Integer selectedOrderCount;
    private Integer selectedSalesOrderCount;
    private Integer taskCount;
    private Integer totalQuantity;
    private Integer wholePalletCount;
    private Integer secondaryOrderCount;
    private List<PickTaskPreviewVO> tasks;
}
