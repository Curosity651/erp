package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuService;
import com.erp.admin.wms.converter.InventoryConverter;
import com.erp.admin.wms.mapper.InventoryMapper;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.qo.InventoryQO;
import com.erp.admin.wms.model.vo.AvailableStockVO;
import com.erp.admin.wms.model.vo.InventoryDetailVO;
import com.erp.admin.wms.model.vo.InventoryPageVO;
import com.erp.admin.wms.model.vo.InventorySummaryVO;
import com.erp.admin.wms.model.vo.SkuSummaryVO;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.WarehouseSummaryVO;
import com.erp.admin.wms.util.VolumeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 库存服务
 * 新架构下只负责库存快照的 CRUD 和查询，不包含业务逻辑
 * 库存变更统一通过 StockPostingService.post() 触发
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService extends ExtendServiceImpl<InventoryMapper, Inventory> {

    private final StockFlowService stockFlowService;

    private final WarehouseService warehouseService;

    private final SkuBriefService skuBriefService;

    private final SkuService skuService;

    // ==================== 核心方法（供 Engine 调用）====================

    /**
     * 获取或创建库存记录（按货主隔离）。
     * 用于 InventoryPostingEngine 在执行仓库级过账时获取库存快照。
     * <p>
     * 必须指定 erpTenantId（货主）：wms_inventory 唯一键为 (wms_tenant, erp_tenant, warehouse, sku)，
     * 聚合器（上架）按该键建行。若仍按 (warehouse, sku) 盲查，多货主同仓同 SKU 时会 selectOne 抛
     * TooManyResults 或把两个货主的数量混算到同一行。这里按 (erp, warehouse, sku) 命中聚合器所建行
     * （忽略 wms 维，1 货主→1 父服务商保证唯一）；确无则新建（wms 维暂置 0，与当前上架口径一致）。
     *
     * @param erpTenantId 货主（必填）
     * @param warehouseId 仓库ID
     * @param skuCode SKU编码
     * @return 库存记录（新建时四个库存桶均为0）
     */
    public Inventory getOrCreate(Long erpTenantId, Long warehouseId, String skuCode) {
        Assert.notNull(erpTenantId, "仓库级库存过账必须指定货主 erpTenantId，warehouseId=" + warehouseId + ", skuCode=" + skuCode);
        Inventory inventory = baseMapper.selectByErpWarehouseSku(erpTenantId, warehouseId, skuCode);
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setWmsTenantId(0L);
            inventory.setErpTenantId(erpTenantId);
            inventory.setWarehouseId(warehouseId);
            inventory.setSkuCode(skuCode);
            inventory.setAvailableQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setInTransitQuantity(0);
			inventory.setDamagedQuantity(0);
            inventory.setVersion(0);
            this.save(inventory);
            log.info("创建库存记录，erpTenantId={}，warehouseId={}，skuCode={}", erpTenantId, warehouseId, skuCode);
        }
        return inventory;
    }

    // ==================== 查询方法（供业务层调用）====================

    /**
     * 根据仓库ID和SKU编码查询库存
     */
    public Inventory getByWarehouseAndSku(Long warehouseId, String skuCode) {
        return baseMapper.selectByWarehouseAndSku(warehouseId, skuCode);
    }

    /**
     * 按 (货主, 仓, SKU) 查询库存快照（仓库级库存按货主隔离后校验/取数用；避免 2 键 selectOne 抛错）。
     */
    public Inventory getByErpWarehouseAndSku(Long erpTenantId, Long warehouseId, String skuCode) {
        return baseMapper.selectByErpWarehouseSku(erpTenantId, warehouseId, skuCode);
    }

    /**
     * 按 (货主, 仓, SKU 列表) 批量取库存 Map（按货主隔离，避免跨货主折叠）。
     */
    public Map<String, Inventory> getStockMapByErpWarehouseAndSkuCodes(Long erpTenantId, Long warehouseId,
            List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return baseMapper.selectByErpWarehouseAndSkuCodes(erpTenantId, warehouseId, skuCodes)
                .stream()
                .collect(Collectors.toMap(Inventory::getSkuCode, inv -> inv, (a, b) -> a));
    }

    /**
     * 根据仓库ID查询所有有库存的记录（库存>0）
     */
    public List<Inventory> getByWarehouseId(Long warehouseId) {
        return baseMapper.selectByWarehouseId(warehouseId);
    }

    /**
     * 获取仓库所有有记录的库存（含数量为0的）
     * 用于盘点时需要覆盖所有曾经有库存记录的SKU
     *
     * @param warehouseId 仓库ID
     * @return 库存列表
     */
    public List<Inventory> getAllByWarehouseId(Long warehouseId) {
        return baseMapper.selectAllByWarehouseId(warehouseId);
    }

    /**
     * 根据仓库ID和SKU编码列表批量查询库存，返回 Map
     *
     * @param warehouseId 仓库ID
     * @param skuCodes    SKU编码列表
     * @return Map&lt;skuCode, Inventory&gt;
     */
    public Map<String, Inventory> getStockMapByWarehouseAndSkuCodes(Long warehouseId, List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return baseMapper.selectByWarehouseAndSkuCodes(warehouseId, skuCodes)
                .stream()
                .collect(Collectors.toMap(
                        Inventory::getSkuCode,
                        inv -> inv,
                        (a, b) -> a
                ));
    }

    /**
     * 根据仓库ID集合和SKU关键词查询库存
     * <p>
     * 用于库存预测汇总查询
     *
     * @param warehouseIds 仓库ID集合
     * @param skuKeyword SKU关键词（模糊匹配）
     * @return 库存列表
     */
    public List<Inventory> listByWarehousesAndKeyword(java.util.Set<Long> warehouseIds, String skuKeyword) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectByWarehousesAndKeyword(warehouseIds, skuKeyword);
    }

    /**
     * 批量查询可用库存数量
     * @param warehouseId 仓库ID
     * @param skuCodes SKU编码列表
     * @return Map<skuCode, availableQuantity>
     */
    public Map<String, Integer> getAvailableStockMap(Long warehouseId, List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return baseMapper.selectByWarehouseAndSkuCodes(warehouseId, skuCodes)
                .stream()
                .collect(Collectors.toMap(
                        Inventory::getSkuCode,
                        Inventory::getAvailableQuantity,
                        (a, b) -> a
                ));
    }

    /**
     * 分页查询可调拨库存
     * @param pageParam 分页参数
     * @param warehouseId 仓库ID
     * @param keyword 关键字（SKU编码或名称）
     * @return 分页结果
     */
    public PageResult<AvailableStockVO> pageAvailableStock(PageParam pageParam, Long warehouseId, String keyword) {
        IPage<AvailableStockVO> page = baseMapper.selectAvailableStockPage(
                PageUtil.prodPage(pageParam), warehouseId, keyword);

        List<AvailableStockVO> records = page.getRecords();
        if (!records.isEmpty()) {
            skuBriefService.enrichForQuery(
                    records,
                    AvailableStockVO::getSkuCode,
                    AvailableStockVO::setSkuBrief
            );
        }
        return new PageResult<>(records, page.getTotal());
    }

    /**
     * WMS 服务商货架库存只读汇总（数据级可见性 ③）：按当前服务商 wms_tenant_id 返回桶汇总（不含库位明细）。
     * @return 库存聚合快照列表（仅自己货架上的货）
     */
    public List<Inventory> listByCurrentOperator() {
        Long operatorId = com.erp.admin.common.tenant.WmsTenantContext.getCurrentWmsTenant();
        if (operatorId == null) {
            return Collections.emptyList();
        }
        // C3：按服务商名下货主(sys_tenant.parent_wms_tenant_id)聚合，替代 M-1 后恒空的 wms_tenant_id 过滤
        return baseMapper.selectByOperatorOwnership(operatorId);
    }

    // ==================== 统计查询方法 ====================

    /**
     * 当前请求的货主作用域（数据级可见性）：货主=自身 id；平台超管=-1 哨兵→0 行；
     * WMS 服务商=自身运营商 id→0 行（服务商走 selectByWmsTenant 汇总视图）。 Web 已登录请求 TenantContext 必非空。
     */
    private Long currentErpScope() {
        Long scope = com.erp.admin.common.tenant.TenantContext.getCurrentTenant();
        return scope == null ? com.erp.admin.common.tenant.TenantContext.BLOCK_TENANT_ID : scope;
    }

    /**
     * 获取库存汇总统计
     */
    public InventorySummaryVO getSummary() {
        return baseMapper.selectSummary(currentErpScope());
    }

    /**
     * 按仓库汇总
     */
    public List<WarehouseSummaryVO> getSummaryByWarehouse(String warehouseType) {
        List<WarehouseSummaryVO> list = baseMapper.selectSummaryByWarehouse(warehouseType, currentErpScope());
        // 填充仓库展示信息
        warehouseService.enrichWarehouseDisplay(
                list,
                WarehouseSummaryVO::getWarehouseId,
                WarehouseSummaryVO::setWarehouseDisplay
        );
        return list;
    }

    /**
     * 按SKU汇总（分页，含体积）
     */
    public PageResult<SkuSummaryVO> getSummaryBySku(PageParam pageParam, String keyword, String stockStatus) {
        IPage<SkuSummaryVO> page = PageUtil.prodPage(pageParam);
        baseMapper.selectSummaryBySku(page, keyword, stockStatus, currentErpScope());

        List<SkuSummaryVO> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(records, page.getTotal());
        }

        // 填充 SKU 展示信息
        skuBriefService.enrichForQuery(
                records,
                SkuSummaryVO::getSkuCode,
                SkuSummaryVO::setSkuBrief
        );

        // 批量查询 SKU 尺寸并填充体积
        Set<String> skuCodes = records.stream()
                .map(SkuSummaryVO::getSkuCode)
                .collect(Collectors.toSet());
        Map<String, Sku> skuMap = skuService.getSkuMapByCodes(skuCodes);

        for (SkuSummaryVO vo : records) {
            Sku sku = skuMap.get(vo.getSkuCode());
            BigDecimal unitVolume = VolumeCalculator.calculateUnitVolume(sku);
            vo.setUnitVolume(unitVolume);
            if (unitVolume != null) {
                vo.setTotalVolume(VolumeCalculator.calculateTotalVolume(unitVolume, vo.getWarehouseQuantity()));
            }
        }

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 库存明细分页查询
     */
    public PageResult<InventoryPageVO> queryPage(PageParam pageParam, InventoryQO qo) {
        // 数据级可见性：强制按当前身份的货主作用域过滤（忽略前端传入）
        qo.setErpTenantId(currentErpScope());
        IPage<Inventory> page = baseMapper.queryPage(PageUtil.prodPage(pageParam), qo);

        // Entity -> VO 转换
        List<InventoryPageVO> records = InventoryConverter.INSTANCE.entityListToPageVOList(page.getRecords());
        // 填充关联数据
        enrichPageVOList(records);

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 获取库存明细详情（含最近流水）
     */
    public InventoryDetailVO getDetail(Long warehouseId, String skuCode) {
        // 数据级可见性：按当前货主作用域精确取本货主的库存行（避免多货主同仓同 SKU 时 2 键 selectOne 抛错）
        Long scope = currentErpScope();
        Inventory inventory = baseMapper.selectByErpWarehouseSku(scope, warehouseId, skuCode);
        Assert.notNull(inventory, "库存记录不存在");

        // Entity -> VO 转换
        InventoryDetailVO detail = InventoryConverter.INSTANCE.entityToDetailVO(inventory);
		// 填充仓库信息
		warehouseService.enrichWarehouseDisplay(
				Collections.singletonList(detail),
				InventoryDetailVO::getWarehouseId,
				InventoryDetailVO::setWarehouseDisplay
		);

		// 填充 SKU 展示信息
		skuBriefService.enrichForQuery(
				Collections.singletonList(detail),
				InventoryDetailVO::getSkuCode,
				InventoryDetailVO::setSkuBrief
		);

        // 查询最近10条流水
        List<StockFlowDetailVO> recentFlows = stockFlowService.getRecentFlowsVO(warehouseId, skuCode, 10);
        detail.setRecentFlows(recentFlows);

        return detail;
    }

    // ==================== 数据填充方法 ====================

    /**
     * 填充分页 VO 列表的关联数据
     */
    private void enrichPageVOList(List<InventoryPageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 填充仓库信息
        warehouseService.enrichWarehouseDisplay(
                records,
                InventoryPageVO::getWarehouseId,
				InventoryPageVO::setWarehouseDisplay

		);

        // 填充 SKU 展示信息
		skuBriefService.enrichForQuery(
                records,
                InventoryPageVO::getSkuCode,
				InventoryPageVO::setSkuBrief
        );
    }

    // ==================== FBO 库存同步方法 ====================

    /**
     * 同步 FBO 库存（直接覆盖，不记录流水）
     * <p>
     * FBO 库存由平台控制，ERP 仅做数据同步，不走过账流程。
     *
     * @param warehouseId 仓库ID
     * @param skuCode SKU编码
     * @param quantity 可用库存数量
     * @param syncTime 同步时间
     */
    public void syncFboStock(Long warehouseId, String skuCode, Integer quantity, LocalDateTime syncTime) {
        // FBO 同步按店铺 runAs(货主) 执行，货主取自当前租户上下文
        Long erpTenantId = com.erp.admin.common.tenant.TenantContext.getCurrentTenant();
        Inventory inventory = getOrCreate(erpTenantId, warehouseId, skuCode);
        inventory.setAvailableQuantity(quantity);
        inventory.setSyncTime(syncTime);
        this.updateById(inventory);
    }

    /**
     * 批量同步 FBO 库存
     *
     * @param warehouseId 仓库ID
     * @param stockMap SKU编码 -> 库存数量
     * @param syncTime 同步时间
     */
    public void batchSyncFboStock(Long warehouseId, Map<String, Integer> stockMap, LocalDateTime syncTime) {
        if (stockMap == null || stockMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            syncFboStock(warehouseId, entry.getKey(), entry.getValue(), syncTime);
        }
    }

}
