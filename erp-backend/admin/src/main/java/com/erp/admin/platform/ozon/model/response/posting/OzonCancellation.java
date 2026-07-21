package com.erp.admin.platform.ozon.model.response.posting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ozon 取消信息模型
 * 
 * @author system
 */
@Data
public class OzonCancellation {
    
    /**
     * 取消原因 ID
     */
    @JsonProperty("cancel_reason_id")
    private Long cancelReasonId;
    
    /**
     * 取消原因
     */
    @JsonProperty("cancel_reason")
    private String cancelReason;
    
    /**
     * 取消类型
     */
    @JsonProperty("cancellation_type")
    private String cancellationType;
    
    /**
     * 是否在发货后取消
     */
    @JsonProperty("cancelled_after_ship")
    private Boolean cancelledAfterShip;
    
    /**
     * 是否影响取消评级
     */
    @JsonProperty("affect_cancellation_rating")
    private Boolean affectCancellationRating;
    
    /**
     * 取消发起方
     */
    @JsonProperty("cancellation_initiator")
    private String cancellationInitiator;
}
