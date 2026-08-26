package com.erp.admin.finance.settlement.mapper;

import com.erp.admin.finance.settlement.model.entity.WmsRechargeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.time.LocalDateTime;

public interface WmsRechargeOrderMapper extends ExtendMapper<WmsRechargeOrder> {

    @Update("UPDATE wms_recharge_order SET status=#{toStatus}, reviewer_id=#{reviewerId}, "
            + "reviewer_name=#{reviewerName}, review_time=#{reviewTime}, reject_reason=#{reason} "
            + "WHERE id=#{id} AND status=#{fromStatus}")
    int transition(@Param("id") Long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("reviewerId") Long reviewerId,
            @Param("reviewerName") String reviewerName, @Param("reviewTime") LocalDateTime reviewTime,
            @Param("reason") String reason);

    @Update("UPDATE wms_recharge_order SET status='REVERSED', reviewer_id=#{reviewerId}, "
            + "reviewer_name=#{reviewerName}, review_time=#{reviewTime}, reverse_reason=#{reason}, "
            + "reverse_order_id=#{reverseOrderId} WHERE id=#{id} AND status='APPROVED'")
    int reverseApproved(@Param("id") Long id, @Param("reverseOrderId") Long reverseOrderId,
            @Param("reviewerId") Long reviewerId, @Param("reviewerName") String reviewerName,
            @Param("reviewTime") LocalDateTime reviewTime, @Param("reason") String reason);
}
