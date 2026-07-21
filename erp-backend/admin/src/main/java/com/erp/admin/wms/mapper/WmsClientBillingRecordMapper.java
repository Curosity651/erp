package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsClientBillingRecord;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 客户计费流水 Mapper。
 *
 * @author erp
 */
public interface WmsClientBillingRecordMapper extends ExtendMapper<WmsClientBillingRecord> {

    /** 按业务幂等键查（判重，防止同一单据重复计费） */
    default WmsClientBillingRecord selectByBizId(String bizId) {
        return this.selectOne(WrappersX.lambdaQueryX(WmsClientBillingRecord.class)
            .eq(WmsClientBillingRecord::getBizId, bizId)
            .last("LIMIT 1"));
    }

}
