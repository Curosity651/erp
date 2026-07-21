package com.erp.admin.order.model.dto.wildberries;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WbSupplySyncDTO {
    private String supplyId;
    private String name;
    private String labelBase64;
    private LocalDateTime createdAt;
}


