package com.erp.admin.order.model.vo;

import java.util.List;

import lombok.Data;

@Data
public class LabelUpdateResult {

    private int totalOrders;
    private int processedOrders;
    private int successCount;
    private int skippedCount;
    private int failedCount;
    private List<LabelUpdateItem> items;
}



