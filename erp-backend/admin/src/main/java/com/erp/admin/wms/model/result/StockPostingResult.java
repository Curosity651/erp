package com.erp.admin.wms.model.result;

import lombok.Builder;
import lombok.Data;

/**
 * 库存过账结果
 *
 * @author erp
 */
@Data
@Builder
public class StockPostingResult {

    /** 过账单ID */
    private Long postingId;

    /** 过账单号 */
    private String postingNo;

    /** 是否为已存在的过账单(幂等命中) */
    private boolean existed;

}
