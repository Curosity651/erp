package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.SalesOutboundConverter;
import com.erp.admin.wms.converter.SalesOutboundItemConverter;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.model.dto.SalesOutboundDTO;
import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.qo.SalesOutboundQO;
import com.erp.admin.wms.model.vo.SalesOutboundDetailVO;
import com.erp.admin.wms.model.vo.SalesOutboundExportVO;
import com.erp.admin.wms.model.vo.SalesOutboundItemVO;
import com.erp.admin.wms.model.vo.SalesOutboundPageVO;
import com.erp.admin.wms.model.enums.StockStatus;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售出库单服务
 * <p>
 * 专注于出库单领域的 CRUD 和状态管理。
 * 跨服务编排在 SalesOutboundFacade 中完成。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOutboundService extends ExtendServiceImpl<SalesOutboundMapper, SalesOutboundOrder> {

    public SalesOutboundOrder getByIdForUpdate(Long id) {
        SalesOutboundOrder order = baseMapper.selectByIdForUpdate(id);
        Assert.notNull(order, "销售出库单不存在");
        return order;
    }

    private final SalesOutboundItemService salesOutboundItemService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final StringRedisTemplate stringRedisTemplate;
	private final SkuBriefService skuBriefService;
	private final WarehouseService warehouseService;

	private static final String OUTBOUND_NO_KEY = "wms:sales_outbound:seq:";

	/**
     * 分页查询
     */
    public PageResult<SalesOutboundPageVO> queryPage(PageParam pageParam, SalesOutboundQO qo) {
        IPage<SalesOutboundPageVO> page = PageUtil.prodPage(pageParam);
        baseMapper.queryPage(page, qo);
		List<SalesOutboundPageVO> records = page.getRecords();
		if (CollectionUtils.isEmpty(records)) {
			return new PageResult<>(records, page.getTotal());
		}
		// 填充仓库名称
		List<Long> warehouseIds = records.stream().map(SalesOutboundPageVO::getWarehouseId).collect(Collectors.toList());
		Map<Long, String> warehouseNameMap = this.warehouseService.getNameMapByIds(warehouseIds);
		records.forEach(record -> record.setWarehouseName(warehouseNameMap.get(record.getWarehouseId())));
		return new PageResult<>(records, page.getTotal());
    }

    /**
     * 获取出库单详情
     */
    public SalesOutboundDetailVO getDetail(Long id) {
        SalesOutboundDetailVO detail = baseMapper.selectDetailById(id);
        Assert.notNull(detail, "出库单不存在");

		// 填充仓库名称
		String warehouseName = this.warehouseService.getNameById(detail.getWarehouseId());
		detail.setWarehouseName(warehouseName);

		List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
		List<SalesOutboundItemVO> itemVOS = SalesOutboundItemConverter.INSTANCE.entityListToVOList(items);
		detail.setItems(itemVOS);

		// 填充sku信息
		skuBriefService.enrichForQuery(itemVOS, SalesOutboundItemVO::getSkuCode, SalesOutboundItemVO::setSkuBrief);

        // 填充库存状态（按本单货主取库存，防多货主共享自有仓时跨货主串数——H-7）
        SalesOutboundOrder order = this.getById(id);
        Long erpTenantId = order == null ? null : order.getErpTenantId();
        enrichItemsWithStockStatus(detail, erpTenantId);

        return detail;
    }

    /**
     * 填充明细项的库存状态（按本单货主 erpTenantId 隔离取数）
     */
    private void enrichItemsWithStockStatus(SalesOutboundDetailVO detail, Long erpTenantId) {
        if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
            return;
        }

        // 提交仓库后库存属于本单预留；最终签出后才是真正扣减。
        Set<String> reservedStatuses = new HashSet<>(Arrays.asList(
                OutboundOrderStatus.CONFIRMED.name(),
                OutboundOrderStatus.PICKING.name(),
                OutboundOrderStatus.PICKED.name(),
                OutboundOrderStatus.PACKED.name(),
                OutboundOrderStatus.BACKORDER.name()));
        if (reservedStatuses.contains(detail.getOrderStatus())) {
            for (SalesOutboundItemVO item : detail.getItems()) {
                item.setStockStatus(StockStatus.RESERVED.getValue());
                item.setAvailableStock(null);
                item.setShortage(0);
            }
            detail.setHasStockShortage(false);
            detail.setShortageSkuCount(0);
            return;
        }
        if (OutboundOrderStatus.SHIPPED.name().equals(detail.getOrderStatus())
                || OutboundOrderStatus.COMPLETED.name().equals(detail.getOrderStatus())) {
            for (SalesOutboundItemVO item : detail.getItems()) {
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
                .map(SalesOutboundItemVO::getSkuCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Integer> stockMap = physicalInventoryService.getAllocatableQuantityMap(
                erpTenantId, detail.getWarehouseId(), skuCodes);

        // 按 SKU 汇总需求数量
        Map<String, Integer> requiredMap = detail.getItems().stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundItemVO::getSkuCode,
                        Collectors.summingInt(SalesOutboundItemVO::getQuantity)
                ));

        for (SalesOutboundItemVO item : detail.getItems()) {
            String skuCode = item.getSkuCode();
            int available = stockMap.getOrDefault(skuCode, 0);
            int totalRequired = requiredMap.getOrDefault(skuCode, 0);
            int shortage = StockStatus.calcShortage(available, totalRequired);

            item.setAvailableStock(available);
            item.setShortage(shortage);
            item.setStockStatus(StockStatus.calc(available, totalRequired).getValue());
        }

        // 去重统计不足的 SKU 数量
        long uniqueShortageCount = detail.getItems().stream()
                .filter(item -> item.getShortage() != null && item.getShortage() > 0)
                .map(SalesOutboundItemVO::getSkuCode)
                .distinct()
                .count();

        detail.setHasStockShortage(uniqueShortageCount > 0);
        detail.setShortageSkuCount((int) uniqueShortageCount);
    }

    /**
     * 根据ID获取出库单，不存在则抛异常
     */
    public SalesOutboundOrder getByIdOrThrow(Long id) {
        SalesOutboundOrder order = this.getById(id);
        Assert.notNull(order, "出库单不存在");
        // 与自定义出库单共表（source_type=CUSTOM），销售侧操作不得误改自定义单
        Assert.isTrue(!com.erp.admin.wms.model.enums.OutboundSourceType.CUSTOM.name().equals(order.getSourceType()),
                "该单据为自定义出库单，请在自定义出库单页面操作");
        return order;
    }

    /**
     * 保存出库单（仅保存，不处理订单状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrder(SalesOutboundDTO dto) {
        Assert.notEmpty(dto.getItems(), "出库明细不能为空");

        String outboundNo = generateOutboundNo();

        SalesOutboundOrder order = SalesOutboundConverter.INSTANCE.dtoToEntity(dto);
		if (!"OWNER_PROVIDED".equals(order.getDocumentMode())) {
			order.setDocumentMode("WAREHOUSE_PRINT");
		}
        order.setOutboundNo(outboundNo);
        // 显式标记来源类型，与自定义出库单（CUSTOM）区分
        order.setSourceType(com.erp.admin.wms.model.enums.OutboundSourceType.SALES.name());
        order.setOrderStatus(OutboundOrderStatus.DRAFT.name());

        calculateStatistics(order, dto.getItems());

        this.save(order);

        salesOutboundItemService.batchSave(order.getId(), dto.getItems());

        log.info("Saved sales outbound order, id={}, outboundNo={}", order.getId(), outboundNo);
        return order.getId();
    }

    /**
     * 更新出库单（仅更新，不处理订单状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(SalesOutboundDTO dto) {
        SalesOutboundOrder order = getByIdOrThrow(dto.getId());

        // 删除原有明细
        salesOutboundItemService.deleteByOutboundOrderId(order.getId());

        // 更新出库单（platform 不可修改）
        order.setWarehouseId(dto.getWarehouseId());
        order.setOutboundDate(dto.getOutboundDate());
        order.setRemark(dto.getRemark());
        order.setLogisticsProductId(dto.getLogisticsProductId());
		order.setDocumentMode("OWNER_PROVIDED".equals(dto.getDocumentMode())
				? "OWNER_PROVIDED" : "WAREHOUSE_PRINT");

        calculateStatistics(order, dto.getItems());

        this.updateById(order);

        // 保存新明细
        salesOutboundItemService.batchSave(order.getId(), dto.getItems());

        log.info("Updated sales outbound order, id={}", order.getId());
    }

    /**
     * 删除出库单（仅删除，不处理订单状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        salesOutboundItemService.deleteByOutboundOrderId(id);
        this.removeById(id);
        log.info("Deleted sales outbound order, id={}", id);
    }

    /**
     * 更新为已确认状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateToConfirmed(Long id, Long postingId) {
        SalesOutboundOrder order = getByIdOrThrow(id);
        order.setOrderStatus(OutboundOrderStatus.CONFIRMED.name());
        order.setPostingId(postingId);
        this.updateById(order);
        log.info("Updated sales outbound order to CONFIRMED, id={}", id);
    }

    /**
     * 更新为已取消状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateToCancelled(Long id) {
        SalesOutboundOrder order = getByIdOrThrow(id);
        order.setOrderStatus(OutboundOrderStatus.CANCELLED.name());
        this.updateById(order);
        log.info("Updated sales outbound order to CANCELLED, id={}", id);
    }

    /**
     * 导出出库单列表
     */
    public List<SalesOutboundExportVO> listForExport(SalesOutboundQO qo) {
		List<SalesOutboundExportVO> records = baseMapper.selectListForExport(qo);
		// 填充仓库名称
		List<Long> warehouseIds = records.stream().map(SalesOutboundExportVO::getWarehouseId).collect(Collectors.toList());
		Map<Long, String> warehouseNameMap = this.warehouseService.getNameMapByIds(warehouseIds);
		records.forEach(record -> {
			record.setWarehouseName(warehouseNameMap.get(record.getWarehouseId()));
			// 单据状态 code → 中文
			record.setOrderStatus(translateOrderStatus(record.getOrderStatus()));
		});
		return records;
    }

    /**
     * 出库单状态枚举 code 翻译为中文描述，未匹配则原样返回
     * @param code 状态枚举 name
     * @return 中文描述
     */
    private String translateOrderStatus(String code) {
		if (code == null) {
			return null;
		}
		for (OutboundOrderStatus status : OutboundOrderStatus.values()) {
			if (status.name().equals(code)) {
				return status.getDescription();
			}
		}
		return code;
    }

    /**
     * 计算统计信息
     */
    private void calculateStatistics(SalesOutboundOrder order, List<SalesOutboundItemDTO> items) {
        Set<Long> orderIds = new HashSet<>();
        Set<String> skuCodes = new HashSet<>();
        int totalQuantity = 0;

        for (SalesOutboundItemDTO item : items) {
            orderIds.add(item.getErpOrderId());
            skuCodes.add(item.getSkuCode());
            totalQuantity += item.getQuantity();
        }

        order.setOrderCount(orderIds.size());
        order.setSkuCount(skuCodes.size());
        order.setTotalQuantity(totalQuantity);
    }

    /**
     * 生成出库单号（使用 Redis 原子计数器）
     * 格式：SO + YYYYMMDD + 4位序号
     */
    private String generateOutboundNo() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = OUTBOUND_NO_KEY + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间为2天
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        return String.format("SO%s%04d", today, seq);
    }

}
