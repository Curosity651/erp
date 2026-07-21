package com.erp.admin.platform.wildberries.model.common;

import lombok.Builder;
import lombok.Data;

/**
 * Wildberries API 分页查询参数
 * <p>
 * 用于订单列表、Supply 列表等分页接口
 * 
 * @author system
 */
@Data
@Builder
public class WbPaginationQuery {
    
    /**
     * 每页数量限制
     * 范围：1-1000
     */
    private Integer limit;
    
    /**
     * 下一页游标
     * 首次查询传 0，后续使用响应中的 next 值
     */
    private Long next;
    
    /**
     * 查询起始时间（Unix 时间戳，秒）
     * 可选参数，用于按时间范围查询订单
     * 默认为请求前 30 天
     */
    private Long dateFrom;
    
    /**
     * 查询结束时间（Unix 时间戳，秒）
     * 可选参数，用于按时间范围查询订单
     */
    private Long dateTo;
}
