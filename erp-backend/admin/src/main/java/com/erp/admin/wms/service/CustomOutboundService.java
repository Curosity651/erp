package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.CustomOutboundMapper;
import com.erp.admin.wms.model.dto.CustomOutboundDTO;
import com.erp.admin.wms.model.dto.CustomOutboundItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.enums.CustomOutboundType;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.enums.OutboundSourceType;
import com.erp.admin.wms.model.enums.StockStatus;
import com.erp.admin.wms.model.qo.CustomOutboundQO;
import com.erp.admin.wms.model.vo.CustomOutboundDetailVO;
import com.erp.admin.wms.model.vo.CustomOutboundItemVO;
import com.erp.admin.wms.model.vo.CustomOutboundPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义出库单服务
 * <p>
 * 与销售出库单共用 {@code wms_sales_outbound_order} 表（{@code source_type=CUSTOM}），
 * 专注于自定义出库单领域的 CRUD 和状态管理，跨服务编排在 CustomOutboundFacade 中完成。
 * 明细复用 {@code wms_sales_outbound_order_item} 表（不关联 erp_order，直接 SKU + 数量）。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOutboundService extends ExtendServiceImpl<CustomOutboundMapper, SalesOutboundOrder> {

    private final SalesOutboundItemService salesOutboundItemService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SkuBriefService skuBriefService;
    private final WarehouseService warehouseService;

    private static final String OUTBOUND_NO_KEY = "wms:custom_outbound:seq:";

    /**
     * 分页查询
     */
    public PageResult<CustomOutboundPageVO> queryPage(PageParam pageParam, CustomOutboundQO qo) {
        IPage<CustomOutboundPageVO> page = PageUtil.prodPage(pageParam);
        baseMapper.queryPage(page, qo);
        List<CustomOutboundPageVO> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new PageResult<>(records, page.getTotal());
        }
        // 填充仓库名称
        fillWarehouseName(records);
        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 获取出库单详情
     */
    public CustomOutboundDetailVO getDetail(Long id) {
        CustomOutboundDetailVO detail = baseMapper.selectDetailById(id);
        Assert.notNull(detail, "出库单不存在");

        // 填充仓库名称
        detail.setWarehouseName(this.warehouseService.getNameById(detail.getWarehouseId()));

        // 填充明细
        List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
        List<CustomOutboundItemVO> itemVOS = items.stream().map(item -> {
            CustomOutboundItemVO vo = new CustomOutboundItemVO();
            vo.setId(item.getId());
            vo.setOutboundOrderId(item.getOutboundOrderId());
            vo.setSkuCode(item.getSkuCode());
            vo.setQuantity(item.getQuantity());
            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
        detail.setItems(itemVOS);

        // 填充sku信息
        skuBriefService.enrichForQuery(itemVOS, CustomOutboundItemVO::getSkuCode, CustomOutboundItemVO::setSkuBrief);

        // 填充库存状态（按本单货主取库存，防多货主共享自有仓时跨货主串数——H-7）
        SalesOutboundOrder order = this.getById(id);
        Long erpTenantId = order == null ? null : order.getErpTenantId();
        enrichItemsWithStockStatus(detail, erpTenantId);

        return detail;
    }

    /**
     * 填充明细项的库存状态（草稿态展示可售余量；已提交及之后已完成预占，标记为已扣减）。
     * 按本单货主 erpTenantId 隔离取数。
     */
    private void enrichItemsWithStockStatus(CustomOutboundDetailVO detail, Long erpTenantId) {
        if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
            return;
        }

        // 已取消：不展示库存状态
        if (OutboundOrderStatus.CANCELLED.name().equals(detail.getOrderStatus())) {
            detail.setHasStockShortage(false);
            detail.setShortageSkuCount(0);
            return;
        }

        // 非草稿状态（已提交/作业中/完成等），库存已预占扣减
        if (!OutboundOrderStatus.DRAFT.name().equals(detail.getOrderStatus())) {
            for (CustomOutboundItemVO item : detail.getItems()) {
                item.setStockStatus(StockStatus.DEDUCTED.getValue());
                item.setAvailableStock(null);
                item.setShortage(0);
            }
            detail.setHasStockShortage(false);
            detail.setShortageSkuCount(0);
            return;
        }

        // 草稿状态：查询当前库存
        List<String> skuCodes = detail.getItems().stream()
                .map(CustomOutboundItemVO::getSkuCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Integer> stockMap = physicalInventoryService.getAllocatableQuantityMap(
                erpTenantId, detail.getWarehouseId(), skuCodes);

        // 按 SKU 汇总需求数量
        Map<String, Integer> requiredMap = detail.getItems().stream()
                .collect(Collectors.groupingBy(
                        CustomOutboundItemVO::getSkuCode,
                        Collectors.summingInt(CustomOutboundItemVO::getQuantity)
                ));

        for (CustomOutboundItemVO item : detail.getItems()) {
            int available = stockMap.getOrDefault(item.getSkuCode(), 0);
            int totalRequired = requiredMap.getOrDefault(item.getSkuCode(), 0);
            item.setAvailableStock(available);
            item.setShortage(StockStatus.calcShortage(available, totalRequired));
            item.setStockStatus(StockStatus.calc(available, totalRequired).getValue());
        }

        long uniqueShortageCount = detail.getItems().stream()
                .filter(item -> item.getShortage() != null && item.getShortage() > 0)
                .map(CustomOutboundItemVO::getSkuCode)
                .distinct()
                .count();
        detail.setHasStockShortage(uniqueShortageCount > 0);
        detail.setShortageSkuCount((int) uniqueShortageCount);
    }

    /**
     * 根据ID获取自定义出库单，不存在或非 CUSTOM 则抛异常
     */
    public SalesOutboundOrder getByIdOrThrow(Long id) {
        SalesOutboundOrder order = this.getById(id);
        Assert.notNull(order, "出库单不存在");
        Assert.isTrue(OutboundSourceType.CUSTOM.name().equals(order.getSourceType()), "该单据不是自定义出库单");
        return order;
    }

    /**
     * 保存出库单草稿（仅保存，库存校验与预占在 Facade.submit 中完成）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrder(CustomOutboundDTO dto) {
        Assert.notEmpty(dto.getItems(), "出库明细不能为空");
        validateCustomType(dto.getCustomType());

        SalesOutboundOrder order = new SalesOutboundOrder();
        applyDtoFields(order, dto);
        order.setOutboundNo(generateOutboundNo());
        order.setSourceType(OutboundSourceType.CUSTOM.name());
        order.setOrderStatus(OutboundOrderStatus.DRAFT.name());

        calculateStatistics(order, dto.getItems());

        this.save(order);

        batchSaveItems(order.getId(), dto.getItems());

        log.info("Saved custom outbound order, id={}, outboundNo={}", order.getId(), order.getOutboundNo());
        return order.getId();
    }

    /**
     * 更新出库单草稿（仅更新草稿内容）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(CustomOutboundDTO dto) {
        SalesOutboundOrder order = getByIdOrThrow(dto.getId());
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以编辑");
        validateCustomType(dto.getCustomType());

        // 删除原有明细
        salesOutboundItemService.deleteByOutboundOrderId(order.getId());

        // 更新出库单
        applyDtoFields(order, dto);
        calculateStatistics(order, dto.getItems());

        this.updateById(order);

        // 保存新明细
        batchSaveItems(order.getId(), dto.getItems());

        log.info("Updated custom outbound order, id={}", order.getId());
    }

    /**
     * 删除出库单（仅删除，不校验状态，状态校验在 Facade 完成）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        salesOutboundItemService.deleteByOutboundOrderId(id);
        this.removeById(id);
        log.info("Deleted custom outbound order, id={}", id);
    }

    /**
     * 更新为已确认状态（提交成功：预占过账后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateToConfirmed(Long id, Long postingId) {
        SalesOutboundOrder order = getByIdOrThrow(id);
        order.setOrderStatus(OutboundOrderStatus.CONFIRMED.name());
        order.setPostingId(postingId);
        this.updateById(order);
        log.info("Updated custom outbound order to CONFIRMED, id={}", id);
    }

    /**
     * 更新为已取消状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateToCancelled(Long id) {
        SalesOutboundOrder order = getByIdOrThrow(id);
        order.setOrderStatus(OutboundOrderStatus.CANCELLED.name());
        this.updateById(order);
        log.info("Updated custom outbound order to CANCELLED, id={}", id);
    }

    /**
     * 导出出库单列表
     */
    public List<CustomOutboundPageVO> listForExport(CustomOutboundQO qo) {
        List<CustomOutboundPageVO> records = baseMapper.selectListForExport(qo);
        fillWarehouseName(records);
        return records;
    }

    /**
     * 填充仓库名称
     */
    private void fillWarehouseName(List<CustomOutboundPageVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> warehouseIds = records.stream().map(CustomOutboundPageVO::getWarehouseId).collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = this.warehouseService.getNameMapByIds(warehouseIds);
        records.forEach(record -> record.setWarehouseName(warehouseNameMap.get(record.getWarehouseId())));
    }

    /**
     * DTO 字段落到实体（单号/来源/状态除外）
     */
    private void applyDtoFields(SalesOutboundOrder order, CustomOutboundDTO dto) {
        order.setWarehouseId(dto.getWarehouseId());
        order.setOutboundDate(dto.getOutboundDate());
        order.setCustomType(dto.getCustomType());
        order.setRefNo(dto.getRefNo());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setLogisticsProductId(dto.getLogisticsProductId());
        order.setRemark(dto.getRemark());
    }

    /**
     * 批量保存明细（不关联 erp_order，erp_order_id 留空）
     */
    private void batchSaveItems(Long outboundOrderId, List<CustomOutboundItemDTO> items) {
        List<SalesOutboundOrderItem> entities = items.stream().map(dto -> {
            SalesOutboundOrderItem entity = new SalesOutboundOrderItem();
            entity.setOutboundOrderId(outboundOrderId);
            // 自定义出库不挂 ERP 订单，但 erp_order_id 列 NOT NULL 无默认值，填哨兵 0（与 orderCount 恒为 0 一致）
            entity.setErpOrderId(0L);
            entity.setSkuCode(dto.getSkuCode());
            entity.setQuantity(dto.getQuantity());
            entity.setRemark(dto.getRemark());
            return entity;
        }).collect(Collectors.toList());
        salesOutboundItemService.saveBatch(entities);
    }

    /**
     * 校验出库类型合法性
     */
    private void validateCustomType(String customType) {
        try {
            CustomOutboundType.valueOf(customType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("非法的出库类型: " + customType);
        }
    }

    /**
     * 计算统计信息（自定义出库不挂订单，orderCount 恒为 0）
     */
    private void calculateStatistics(SalesOutboundOrder order, List<CustomOutboundItemDTO> items) {
        Set<String> skuCodes = new HashSet<>();
        int totalQuantity = 0;
        for (CustomOutboundItemDTO item : items) {
            skuCodes.add(item.getSkuCode());
            totalQuantity += item.getQuantity();
        }
        order.setOrderCount(0);
        order.setSkuCount(skuCodes.size());
        order.setTotalQuantity(totalQuantity);
    }

    /**
     * 生成出库单号（使用 Redis 原子计数器）
     * 格式：CO + YYYYMMDD + 4位序号
     */
    private String generateOutboundNo() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = OUTBOUND_NO_KEY + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间为2天
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        return String.format("CO%s%04d", today, seq);
    }

}
