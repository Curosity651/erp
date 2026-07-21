package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.StockFlow;
import com.erp.admin.wms.model.enums.StockDirection;
import com.erp.admin.wms.model.qo.StockFlowQO;
import com.erp.admin.wms.model.vo.StockFlowTodaySummaryVO;
import com.erp.admin.wms.model.vo.StockFlowTrendVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 库存流水 Mapper
 *
 * @author erp
 */
public interface StockFlowMapper extends ExtendMapper<StockFlow> {

    /**
     * 根据来源单据查询流水
     * @param sourceType 来源单据类型
     * @param sourceId 来源单据ID
     * @return List<StockFlow> 流水列表
     */
    default List<StockFlow> selectBySource(String sourceType, Long sourceId) {
        return selectList(WrappersX.<StockFlow>lambdaQueryX()
                .eq(StockFlow::getSourceType, sourceType)
                .eq(StockFlow::getSourceId, sourceId)
                .orderByDesc(StockFlow::getCreateTime, StockFlow::getId));
    }

    /**
     * 根据过账单ID查询流水
     * @param postingId 过账单ID
     * @return List<StockFlow> 流水列表
     */
    default List<StockFlow> selectByPostingId(Long postingId) {
        return selectList(WrappersX.<StockFlow>lambdaQueryX()
                .eq(StockFlow::getPostingId, postingId)
                .orderByDesc(StockFlow::getCreateTime, StockFlow::getId));
    }

    /**
     * 分页查询
     * @param page 分页对象
     * @param qo 查询条件
     * @return 分页结果（Entity）
     */
    default IPage<StockFlow> queryPage(IPage<StockFlow> page, StockFlowQO qo) {
        LambdaQueryWrapperX<StockFlow> wrapper = WrappersX.<StockFlow>lambdaQueryX()
                .eqIfPresent(StockFlow::getWarehouseId, qo.getWarehouseId())
                .likeIfPresent(StockFlow::getSkuCode, qo.getSkuCode())
                .eqIfPresent(StockFlow::getBucket, qo.getBucket())
                .eqIfPresent(StockFlow::getPostingType, qo.getPostingType())
                .inIfPresent(StockFlow::getPostingType, qo.getPostingTypes())
                .likeIfPresent(StockFlow::getSourceNo, qo.getSourceNo())
                .likeIfPresent(StockFlow::getPostingNo, qo.getPostingNo())
                .geIfPresent(StockFlow::getCreateTime, qo.getStartTime())
                .leIfPresent(StockFlow::getCreateTime, qo.getEndTime())
                .inIfPresent(StockFlow::getErpTenantId, qo.getErpTenantIds());

        // 区域筛选（性能优化版：使用预查询的仓库ID列表）
        if (qo.getRegionId() != null && qo.getRegionWarehouseIds() != null && !qo.getRegionWarehouseIds().isEmpty()) {
            wrapper.in(StockFlow::getWarehouseId, qo.getRegionWarehouseIds());
        }

        // 流水方向筛选（使用 direction 字段）
        if (StockDirection.IN.name().equals(qo.getDirection())) {
            wrapper.eq(StockFlow::getDirection, StockDirection.IN);
        } else if (StockDirection.OUT.name().equals(qo.getDirection())) {
            wrapper.eq(StockFlow::getDirection, StockDirection.OUT);
        }

        wrapper.orderByDesc(StockFlow::getCreateTime, StockFlow::getId);

        return this.selectPage(page, wrapper);
    }

    /**
     * 查询指定仓库+SKU 实际出现过的过账类型（去重），用于筛选下拉动态选项。
     * @param warehouseId 仓库ID（可空）
     * @param skuCode SKU编码（可空）
     * @param erpTenantIds 货主作用域（服务端注入，可空=不过滤）
     * @return 去重后的过账类型编码列表
     */
    default List<String> listDistinctPostingTypes(Long warehouseId, String skuCode, List<Long> erpTenantIds) {
        return this.selectObjs(WrappersX.<StockFlow>lambdaQueryX()
                .select(StockFlow::getPostingType)
                .eqIfPresent(StockFlow::getWarehouseId, warehouseId)
                .eqIfPresent(StockFlow::getSkuCode, skuCode)
                .inIfPresent(StockFlow::getErpTenantId, erpTenantIds)
                .groupBy(StockFlow::getPostingType))
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询最近流水
     * @param warehouseId 仓库ID
     * @param skuCode SKU编码
     * @param limit 数量限制
     * @param erpTenantIds 货主作用域（null=不过滤，仅平台；否则按 IN 收窄，防跨货主串流水）
     * @return 流水列表
     */
    default List<StockFlow> selectRecentFlows(Long warehouseId, String skuCode, int limit, List<Long> erpTenantIds) {
        return selectList(WrappersX.<StockFlow>lambdaQueryX()
                .eq(StockFlow::getWarehouseId, warehouseId)
                .eq(StockFlow::getSkuCode, skuCode)
                .inIfPresent(StockFlow::getErpTenantId, erpTenantIds)
                .orderByDesc(StockFlow::getCreateTime, StockFlow::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 查询今日汇总统计
     * @param warehouseId 仓库ID（可选）
     * @param erpTenantIds 货主作用域（null=不过滤，仅平台）
     * @return 今日汇总统计
     */
    StockFlowTodaySummaryVO selectTodaySummary(@Param("warehouseId") Long warehouseId,
            @Param("erpTenantIds") List<Long> erpTenantIds);

    /**
     * 查询流水趋势数据
     * @param days 天数
     * @param warehouseId 仓库ID（可选）
     * @param erpTenantIds 货主作用域（null=不过滤，仅平台）
     * @return 趋势数据列表
     */
    List<StockFlowTrendVO> selectTrend(@Param("days") Integer days, @Param("warehouseId") Long warehouseId,
            @Param("erpTenantIds") List<Long> erpTenantIds);

}
