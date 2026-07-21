package com.erp.admin.order.model.dto.wildberries;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WbOrderSyncDTO {
    private String orderId;
    private String wbStatus;
    private String supplierStatus;
    private String supplyId;
    private String warehouseId;
    private String officeId;
	private String article;
    private String deliveryType;
    private Integer price;
    private Integer convertedPrice;
    private String currencyCode;
    private String convertedCurrencyCode;
	private String comment;
    private LocalDateTime createdAt;
    private String rawJson;
}


