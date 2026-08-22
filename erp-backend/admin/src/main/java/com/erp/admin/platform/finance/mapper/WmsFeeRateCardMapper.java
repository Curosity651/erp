package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsFeeRateCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WmsFeeRateCardMapper extends ExtendMapper<WmsFeeRateCard> {

    @Select("SELECT * FROM wms_fee_rate_card "
            + "WHERE fee_code=#{feeCode} AND status=1 "
            + "AND (wms_tenant_id=#{wmsTenantId} OR wms_tenant_id=0) "
            + "AND effective_from<=#{date} AND (effective_to IS NULL OR effective_to>=#{date}) "
            + "ORDER BY CASE WHEN wms_tenant_id=#{wmsTenantId} THEN 0 ELSE 1 END, effective_from DESC LIMIT 1")
    WmsFeeRateCard selectEffective(@Param("wmsTenantId") Long wmsTenantId,
            @Param("feeCode") String feeCode, @Param("date") LocalDate date);

    @Select("SELECT * FROM wms_fee_rate_card "
            + "WHERE status=1 AND (wms_tenant_id=#{wmsTenantId} OR wms_tenant_id=0) "
            + "AND effective_from<=#{date} AND (effective_to IS NULL OR effective_to>=#{date}) "
            + "ORDER BY fee_code, CASE WHEN wms_tenant_id=#{wmsTenantId} THEN 0 ELSE 1 END, "
            + "effective_from DESC")
    List<WmsFeeRateCard> listEffectiveCandidates(@Param("wmsTenantId") Long wmsTenantId,
            @Param("date") LocalDate date);
}
