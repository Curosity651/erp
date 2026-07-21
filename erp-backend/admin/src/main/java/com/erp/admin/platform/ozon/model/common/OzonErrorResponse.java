package com.erp.admin.platform.ozon.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Ozon API 错误响应模型
 * <p>
 * 参考: https://docs.ozon.ru/api/seller/zh/#tag/Errors
 * <p>
 * 常见错误代码说明:
 * - "7": Company is blocked, please contact support (账户被封禁)
 * - "401": Unauthorized (认证失败)
 * - "403": Access denied (权限不足)
 * - "404": Not found (资源不存在)
 * - "429": Too many requests (请求过于频繁)
 * - "500": Internal server error (服务器内部错误)
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OzonErrorResponse {
    
    /**
     * 错误代码
     * <p>
     * Ozon API 返回的 code 会被自动转换为字符串
     * 例如: 数字 7 会被转为字符串 "7"
     */
    private String code;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 错误详情列表
     */
    private List<OzonErrorDetail> details;
    
    /**
     * 判断是否为账户被封禁错误
     * <p>
     * 错误代码 "7" 表示 "Company is blocked"
     * 
     * @return 如果是账户被封禁错误则返回 true
     */
    public boolean isCompanyBlocked() {
        return "7".equals(code);
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OzonErrorDetail {
        /**
         * 类型URL
         */
        private String typeUrl;
        
        /**
         * 值
         */
        private String value;
    }
}
