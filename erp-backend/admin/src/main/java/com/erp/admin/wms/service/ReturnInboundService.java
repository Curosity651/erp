package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.ReturnInboundMapper;
import com.erp.admin.wms.mapper.WmsReturnQcItemMapper;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.model.dto.ReturnInboundDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.entity.WmsReturnQcItem;
import com.erp.admin.wms.model.enums.ReturnReason;
import com.erp.admin.wms.model.qo.ReturnInboundQO;
import com.erp.admin.wms.model.qo.ReturnableOrderQO;
import com.erp.admin.wms.model.vo.ReturnInboundDetailVO;
import com.erp.admin.wms.model.vo.ReturnInboundExportVO;
import com.erp.admin.wms.model.vo.ReturnInboundPageVO;
import com.erp.admin.wms.model.vo.ReturnOrderSourceVO;
import com.erp.admin.wms.model.vo.ReturnOrderItemVO;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.model.vo.ReturnableOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.erp.admin.wms.model.enums.ReturnQcStatus;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 退货入库单服务
 * 简化版：单 SKU 场景，无明细表
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnInboundService extends ExtendServiceImpl<ReturnInboundMapper, ReturnInboundOrder> {

    private final SkuBriefService skuBriefService;
    private final TenantIdentityService tenantIdentityService;
    private final SysTenantMapper sysTenantMapper;
    private final WarehouseService warehouseService;
    private final WmsRackAssignmentService wmsRackAssignmentService;
    private final StringRedisTemplate stringRedisTemplate;
    private final WmsReturnQcItemMapper returnQcItemMapper;
    private final WmsSkuLookupMapper skuLookupMapper;
    private final WarehouseSkuCodeService warehouseSkuCodeService;

    /**
     * 分页查询
     * @param pageParam 分页参数
     * @param qo 查询条件
     * @return 分页结果
     */
    public PageResult<ReturnInboundPageVO> queryPage(PageParam pageParam, ReturnInboundQO qo) {
        IPage<ReturnInboundPageVO> page = PageUtil.prodPage(pageParam);
        baseMapper.queryPage(page, qo);

        List<ReturnInboundPageVO> records = page.getRecords();
        if (!records.isEmpty()) {
            // 使用 SkuBriefService 批量填充 skuBrief
            skuBriefService.enrichForQuery(records, ReturnInboundPageVO::getSkuCode, ReturnInboundPageVO::setSkuBrief);
        }

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 获取退货单详情
     * @param id 退货单ID
     * @return 退货单详情
     */
    public ReturnInboundDetailVO getDetail(Long id) {
        ReturnInboundDetailVO detail = baseMapper.selectDetailById(id);
        Assert.notNull(detail, "退货单不存在");
        // 单 SKU 场景，填充 skuBrief
        if (detail.getSkuCode() != null) {
            skuBriefService.enrichForQuery(Collections.singletonList(detail), ReturnInboundDetailVO::getSkuCode, ReturnInboundDetailVO::setSkuBrief);
        }
        ReturnInboundOrder order = baseMapper.selectById(id);
        Assert.notNull(order, "退货处理单不存在");
        List<ReturnOrderItemVO> items = new java.util.ArrayList<>();
        for (WmsReturnQcItem item : returnQcItemMapper.selectByReturnOrderId(id)) {
            ReturnOrderItemVO vo = new ReturnOrderItemVO();
            vo.setId(item.getId());
            vo.setSkuCode(item.getSkuCode());
            vo.setWarehouseSkuCode(warehouseSkuCodeService.build(order.getErpTenantId(), item.getSkuCode()));
            SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(order.getErpTenantId(), item.getSkuCode());
            vo.setSkuName(sku == null ? null : sku.getChineseName());
            vo.setPlatformOrderId(item.getPlatformOrderId());
            vo.setReturnReason(item.getReturnReason());
            vo.setExpectedQty(item.getExpectedQty());
            vo.setReceivedQty(item.getReceivedQty());
            vo.setRestockQty(item.getRestockQty());
            vo.setReworkQty(item.getReworkQty());
            vo.setScrapQty(item.getScrapQty());
            vo.setReworkPassQty(item.getReworkPassQty());
            vo.setReworkScrapQty(item.getReworkScrapQty());
            vo.setDispositionRemark(item.getDispositionRemark());
            vo.setProcessedLocationCode(item.getProcessedLocationCode());
            vo.setQcPhotoFileIds(parsePhotoIds(item.getQcPhotos()));
            items.add(vo);
        }
        detail.setItems(items);
        return detail;
    }

    private List<Long> parsePhotoIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> ids = new java.util.ArrayList<>();
        for (String value : csv.split(",")) {
            try {
                ids.add(Long.valueOf(value.trim()));
            } catch (NumberFormatException ignored) {
                log.warn("忽略非法退货凭证文件ID: {}", value);
            }
        }
        return ids;
    }

    /**
     * 分页查询可退货订单列表
     * 查询已出库（outbound_status = 'COMPLETED'）且有可退数量的订单
     * @param pageParam 分页参数
     * @param qo 查询条件
     * @return 分页结果
     */
    public PageResult<ReturnableOrderVO> getReturnableOrders(PageParam pageParam, ReturnableOrderQO qo) {
        IPage<ReturnableOrderVO> page = PageUtil.prodPage(pageParam);
        baseMapper.selectReturnableOrders(page, qo);

        List<ReturnableOrderVO> records = page.getRecords();
        if (!records.isEmpty()) {
            // 使用 SkuBriefService 批量填充 skuBrief
            skuBriefService.enrichForQuery(records, ReturnableOrderVO::getSkuCode, ReturnableOrderVO::setSkuBrief);
        }

        return new PageResult<>(records, page.getTotal());
    }

    /**
     * 创建退货单（简化版：单 SKU，一步到位）
     * @param dto 退货单DTO
     * @return 退货单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReturnInboundDTO dto) {
        TenantIdentityVO identity = tenantIdentityService.currentIdentity(TenantIdentityService.IDENTITY_ERP_USER);
        Long erpTenantId = identity.getTenantId();

        // Lock the item before calculating availability so concurrent declarations serialize.
        ReturnOrderSourceVO source = baseMapper.selectSourceForUpdate(dto.getOrderItemId(), erpTenantId);
        Assert.notNull(source, "订单商品明细不存在或不属于当前货主");
        Assert.isTrue("COMPLETED".equals(source.getOutboundStatus()), "仅已完成出库的订单可以申报退货");
        Assert.hasText(source.getSkuCode(), "订单商品未配置ERP SKU映射，无法创建退货单");

        Assert.notNull(warehouseService.getById(dto.getWarehouseId()), "入库仓库不存在");
        SysTenant owner = sysTenantMapper.selectById(erpTenantId);
        Assert.notNull(owner, "当前货主不存在");
        Assert.notNull(owner.getParentWmsTenantId(), "当前货主未绑定WMS服务商");
        Assert.isTrue(!wmsRackAssignmentService
                        .activeRackNos(dto.getWarehouseId(), owner.getParentWmsTenantId()).isEmpty(),
                "当前货主无权使用所选仓库");
        validateReturnReason(dto.getReturnReason());

        int alreadyReturned = nz(source.getReturnedQuantity());
        int inFlight = sumInFlightReturnQty(dto.getOrderItemId());
        int actualReturnable = nz(source.getShippedQuantity()) - alreadyReturned - inFlight;
        Assert.isTrue(dto.getQuantity() <= actualReturnable,
                "退货数量不能超过可退数量（含在途未质检退货单占用）");

        ReturnInboundOrder order = new ReturnInboundOrder();
        order.setErpTenantId(erpTenantId);
        order.setReturnNo(generateReturnNo());
        order.setErpOrderId(source.getOrderId());
        order.setOrderItemId(source.getOrderItemId());
        order.setPlatformOrderId(source.getPlatformOrderId());
        order.setPlatform(source.getPlatform());
        order.setSkuCode(source.getSkuCode());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReturnDate(dto.getReturnDate());
        order.setReturnReason(dto.getReturnReason());
        order.setRemark(dto.getRemark());
        order.setTotalQuantity(dto.getQuantity());
        order.setReturnableQuantity(actualReturnable);
        order.setQualifiedQuantity(0);
        order.setUnqualifiedQuantity(0);
        order.setToDamagedQuantity(0);
        order.setScrapQuantity(0);
        order.setReturnStatus(ReturnQcStatus.RETURN_PENDING.name());

        this.save(order);

        log.info("Created return inbound declaration, id={}, returnNo={}, orderItemId={}, qty={}",
                order.getId(), order.getReturnNo(), dto.getOrderItemId(), dto.getQuantity());
        return order.getId();
    }

    private void validateReturnReason(String reason) {
        try {
            ReturnReason.valueOf(reason);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("退货原因不合法");
        }
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 统计某订单明细在途（未质检完成）退货单已占用的退货数量，防止对同一批货重复申报。
     */
    private int sumInFlightReturnQty(Long orderItemId) {
        if (orderItemId == null) {
            return 0;
        }
        List<ReturnInboundOrder> inFlight = baseMapper.selectList(WrappersX.<ReturnInboundOrder>lambdaQueryX()
                .eq(ReturnInboundOrder::getOrderItemId, orderItemId)
                .in(ReturnInboundOrder::getReturnStatus,
                        ReturnQcStatus.RETURN_PENDING.name(), ReturnQcStatus.QC_PENDING.name()));
        return inFlight.stream().mapToInt(o -> o.getTotalQuantity() != null ? o.getTotalQuantity() : 0).sum();
    }

    /**
     * 导出退货单列表
     * @param qo 查询条件
     * @return 导出数据列表
     */
    public List<ReturnInboundExportVO> listForExport(ReturnInboundQO qo) {
        List<ReturnInboundExportVO> records = baseMapper.selectListForExport(qo);
        // 退货原因 code → 中文标签
        for (ReturnInboundExportVO vo : records) {
            vo.setReturnReason(translateReturnReason(vo.getReturnReason()));
        }
        return records;
    }

    /**
     * 退货原因枚举 code 翻译为中文描述，未匹配则原样返回
     * @param code 退货原因枚举 name
     * @return 中文描述
     */
    private String translateReturnReason(String code) {
        if (code == null) {
            return null;
        }
        for (ReturnReason reason : ReturnReason.values()) {
            if (reason.name().equals(code)) {
                return reason.getDescription();
            }
        }
        return code;
    }

    /**
     * 生成退货单号
     * 格式：RI + 年月日 + 4位序号
     * @return 退货单号
     */
    private String generateReturnNo() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "wms:return-inbound:no:" + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        Assert.notNull(seq, "退货单号生成失败");
        return String.format("RI%s%04d", today, seq);
    }

}
