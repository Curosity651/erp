package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.StockPosting;
import com.erp.admin.wms.model.qo.StockPostingQO;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 库存操作单 Mapper
 *
 * @author erp
 */
public interface StockPostingMapper extends ExtendMapper<StockPosting> {

    /**
     * 根据来源单据查询操作单
     * @param sourceType 来源单据类型
     * @param sourceId 来源单据ID
     * @return List<StockIOOrder> 操作单列表
     */
    default List<StockPosting> selectBySource(String sourceType, Long sourceId) {
        return selectList(WrappersX.<StockPosting>lambdaQueryX()
                .eq(StockPosting::getSourceType, sourceType)
                .eq(StockPosting::getSourceId, sourceId)
                .orderByDesc(StockPosting::getId));
    }

    /**
     * 根据过账单号查询（幂等校验用）
     * @param postingNo 过账单号
     * @return StockPosting 过账单
     */
    default StockPosting selectByPostingNo(String postingNo) {
        return selectOne(WrappersX.<StockPosting>lambdaQueryX()
                .eq(StockPosting::getPostingNo, postingNo));
    }

    /**
     * 根据来源单据和过账类型查询（幂等查询）
     * @param sourceType 来源单据类型
     * @param sourceId 来源单据ID
     * @param postingType 过账类型
     * @return 过账单（如存在）
     */
    default StockPosting selectBySourceAndType(String sourceType, Long sourceId, String postingType) {
        return selectOne(WrappersX.<StockPosting>lambdaQueryX()
                .eq(StockPosting::getSourceType, sourceType)
                .eq(StockPosting::getSourceId, sourceId)
                .eq(StockPosting::getPostingType, postingType));
    }

    /**
     * 幂等查询（按货主收窄，M-2）：来源单据 + 过账类型 + 货主。
     * <p>erpTenantId 为空时退化为不带货主维（向后兼容 transfer 等无货主过账，其 source_id 全局唯一无撞号）。
     * @return 过账单（如存在）
     */
    default StockPosting selectBySourceAndType(String sourceType, Long sourceId, String postingType, Long erpTenantId) {
        return selectOne(WrappersX.<StockPosting>lambdaQueryX()
                .eq(StockPosting::getSourceType, sourceType)
                .eq(StockPosting::getSourceId, sourceId)
                .eq(StockPosting::getPostingType, postingType)
                .eqIfPresent(StockPosting::getErpTenantId, erpTenantId));
    }

    /**
     * 分页查询过账单
     * @param page 分页对象
     * @param qo 查询条件
     * @return IPage<StockPosting> 分页结果
     */
    default IPage<StockPosting> queryPage(IPage<StockPosting> page, StockPostingQO qo) {
        LambdaQueryWrapperX<StockPosting> wrapper = WrappersX.<StockPosting>lambdaQueryX()
                .likeIfPresent(StockPosting::getPostingNo, qo.getPostingNo())
                .eqIfPresent(StockPosting::getWarehouseId, qo.getWarehouseId())
                .eqIfPresent(StockPosting::getPostingType, qo.getPostingType())
                .eqIfPresent(StockPosting::getSourceType, qo.getSourceType())
                .likeIfPresent(StockPosting::getSourceNo, qo.getSourceNo())
                .geIfPresent(StockPosting::getPostTime, qo.getPostTimeStart())
                .leIfPresent(StockPosting::getPostTime, qo.getPostTimeEnd())
                .inIfPresent(StockPosting::getErpTenantId, qo.getErpTenantIds())
                .orderByDesc(StockPosting::getId);

        return this.selectPage(page, wrapper);
    }

}
