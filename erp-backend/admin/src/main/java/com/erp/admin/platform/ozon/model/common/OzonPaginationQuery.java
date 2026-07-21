package com.erp.admin.platform.ozon.model.common;

import lombok.Builder;
import lombok.Data;

/**
 * Ozon API 分页查询参数
 * 
 * @author system
 */
@Data
@Builder
public class OzonPaginationQuery {
    
    /**
     * 每页数量限制
     * 范围：1-1000
     */
    private Integer limit;
    
    /**
     * 偏移量
     * 从 0 开始
     */
    private Integer offset;
    
    /**
     * 排序方向
     * ASC 或 DESC
     */
    private String dir;
}
