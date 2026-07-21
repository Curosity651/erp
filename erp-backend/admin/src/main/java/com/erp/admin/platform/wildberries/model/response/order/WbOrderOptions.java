package com.erp.admin.platform.wildberries.model.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wildberries 订单选项
 * <p>
 * 包含订单的额外属性信息
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbOrderOptions {
    
    /** 是否 B2B 订单 */
    @JsonProperty("isB2b")
    private Boolean isB2b;
}
