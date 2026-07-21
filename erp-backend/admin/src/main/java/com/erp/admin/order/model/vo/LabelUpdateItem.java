package com.erp.admin.order.model.vo;

import lombok.Data;

@Data
public class LabelUpdateItem {

    private Long orderId;
    private Long shopId;
    private String platform;
    private String platformOrderId;
    private String supplyId;

    private boolean orderLabelBefore;
    private boolean orderLabelAfter;
    private boolean supplyLabelBefore;
    private boolean supplyLabelAfter;

    private boolean skipped;
    private boolean success;
    private String errorMessage;
}



