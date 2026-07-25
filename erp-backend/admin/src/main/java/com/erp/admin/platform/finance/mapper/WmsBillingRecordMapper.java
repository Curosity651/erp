package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsBillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

@Mapper
public interface WmsBillingRecordMapper extends ExtendMapper<WmsBillingRecord> {

    default WmsBillingRecord selectByBizId(String bizId) {
        return selectOne(WrappersX.lambdaQueryX(WmsBillingRecord.class)
                .eq(WmsBillingRecord::getBizId, bizId)
                .last("LIMIT 1"));
    }
}

