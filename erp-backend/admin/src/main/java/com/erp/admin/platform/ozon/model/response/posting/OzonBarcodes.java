package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 条码信息模型
 * 
 * @author system
 */
@Data
public class OzonBarcodes {
    
    /**
     * 上层条码
     */
    @JsonProperty("upper_barcode")
    private String upperBarcode;
    
    /**
     * 下层条码
     */
    @JsonProperty("lower_barcode")
    private String lowerBarcode;
}
