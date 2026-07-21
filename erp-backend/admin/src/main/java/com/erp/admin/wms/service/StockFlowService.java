package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.StockFlowConverter;
import com.erp.admin.wms.mapper.StockFlowMapper;
import com.erp.admin.wms.model.entity.StockFlow;
import com.erp.admin.wms.model.qo.StockFlowQO;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.StockFlowPageVO;
import com.erp.admin.wms.model.vo.StockFlowTodaySummaryVO;
import com.erp.admin.wms.model.vo.StockFlowTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 库存流水服务
 * 新架构下只负责流水的 CRUD 和查询
 * 流水写入由 InventoryPostingEngine 通过 saveBatch 完成
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockFlowService extends ExtendServiceImpl<StockFlowMapper, StockFlow> {

    private final SkuBriefService skuBriefService;

    private final WarehouseService warehouseService;

    private final RegionService regionService;

    private final ErpOwnerScopeService erpOwnerScopeService;

    // ==================== 查询方法 ====================

    /**
     * 根据来源单据查询流水
     * @param sourceType 来源单据类型
     * @param sourceId 来源单据ID
     * @return List<StockFlow> 流水列表
     */
    public List<StockFlow> getBySource(String sourceType, Long sourceId) {
        return baseMapper.selectBySource(sourceType, sourceId);
    }

    /**
     * 根据过账单ID查询流水
     * @param postingId 过账单ID
     * @return List<StockFlow> 流水列表
     */
    public List<StockFlow> getByPostingId(Long postingId) {
        return baseMapper.selectByPostingId(postingId);
    }

    /**
     * 分页查询
     * @param pageParam 分页参数
     * @param qo 查询条件
     * @return 分页结果
     */
    public PageResult<StockFlowPageVO> queryPage(PageParam pageParam, StockFlowQO qo) {
        // 数据级可见性：按当前身份货主作用域收窄（货主=自身；服务商=名下货主；平台=null不过滤），忽略前端传入
        qo.setErpTenantIds(erpOwnerScopeService.readScope());
        // 性能优化：如果指定了区域，预查询区域下的仓库ID列表
        if (qo.getRegionId() != null) {
            List<Long> warehouseIds = warehouseService.getIdsByRegionId(qo.getRegionId());
            qo.setRegionWarehouseIds(warehouseIds);
        }

        IPage<StockFlow> page = baseMapper.queryPage(PageUtil.prodPage(pageParam), qo);

        // Entity -> VO 转换
        List<StockFlowPageVO> records = StockFlowConverter.INSTANCE.entityListToPageVOList(page.getRecords());
        // 填充关联数据
		if (records == null || records.isEmpty()) {
			return new PageResult<>(new ArrayList<>(), 0);
		}

		// 填充仓库名称
		warehouseService.enrichWarehouseDisplay(
				records,
				StockFlowPageVO::getWarehouseId,
				StockFlowPageVO::setWarehouseDisplay
		);

		// 填充区域展示信息
		regionService.enrichRegionDisplay(
				records,
				StockFlowPageVO::getRegionId,
				StockFlowPageVO::setRegionDisplay
		);

		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(
				records,
				StockFlowPageVO::getSkuCode,
				StockFlowPageVO::setSkuBrief
		);

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 查询指定仓库+SKU 实际出现过的过账类型（去重），用于筛选下拉动态选项。
     * <p>与分页同口径按当前身份货主作用域收窄。
     * @param warehouseId 仓库ID（可空）
     * @param skuCode SKU编码（可空）
     * @return 去重后的过账类型编码列表
     */
    public List<String> listPostingTypes(Long warehouseId, String skuCode) {
        return baseMapper.listDistinctPostingTypes(warehouseId, skuCode, erpOwnerScopeService.readScope());
    }

    /**
     * 获取最近流水（带仓库名称）
     * @param warehouseId 仓库ID
     * @param skuCode SKU编码
     * @param limit 数量限制
     * @return 流水分页VO列表
     */
    public List<StockFlowDetailVO> getRecentFlowsVO(Long warehouseId, String skuCode, int limit) {
        List<StockFlow> flows = baseMapper.selectRecentFlows(warehouseId, skuCode, limit, erpOwnerScopeService.readScope());
        // Entity -> VO 转换
        List<StockFlowDetailVO> records = StockFlowConverter.INSTANCE.entityToDetailVOList(flows);
        // 填充关联数据
		if (records == null || records.isEmpty()) {
			return new ArrayList<>();
		}
		// 填充仓库名称
		warehouseService.enrichWarehouseDisplay(
				records,
				StockFlowDetailVO::getWarehouseId,
				StockFlowDetailVO::setWarehouseDisplay
		);
		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(
				records,
				StockFlowDetailVO::getSkuCode,
				StockFlowDetailVO::setSkuBrief
		);
        return records;
    }

    /**
     * 获取流水详情
     * @param id 流水ID
     * @return 流水详情VO
     */
    public StockFlowDetailVO getDetail(Long id) {
        StockFlow flow = baseMapper.selectById(id);
        if (flow == null) {
            return null;
        }
        // 数据级可见性：非本货主作用域不可见（平台放行）
        if (!erpOwnerScopeService.canAccess(flow.getErpTenantId())) {
            return null;
        }
        // Entity -> VO 转换
        StockFlowDetailVO detail = StockFlowConverter.INSTANCE.entityToDetailVO(flow);
		// 填充仓库展示信息
		if (detail.getWarehouseId() != null) {
			// 填充仓库名称
			warehouseService.enrichWarehouseDisplay(
					Collections.singletonList(detail),
					StockFlowDetailVO::getWarehouseId,
					StockFlowDetailVO::setWarehouseDisplay
			);
		}

		// 填充 SKU 展示信息
		if (detail.getSkuCode() != null) {
			skuBriefService.enrichForQuery(
					Collections.singletonList(detail),
					StockFlowDetailVO::getSkuCode,
					StockFlowDetailVO::setSkuBrief
			);
		}
        return detail;
    }

    /**
     * 获取今日汇总统计
     * @param warehouseId 仓库ID（可选）
     * @return 今日汇总统计
     */
    public StockFlowTodaySummaryVO getTodaySummary(Long warehouseId) {
        return baseMapper.selectTodaySummary(warehouseId, erpOwnerScopeService.readScope());
    }

    /**
     * 获取流水趋势数据
     * @param days 天数
     * @param warehouseId 仓库ID（可选）
     * @return 趋势数据列表
     */
    public List<StockFlowTrendVO> getTrend(Integer days, Long warehouseId) {
        return baseMapper.selectTrend(days, warehouseId, erpOwnerScopeService.readScope());
    }


}
