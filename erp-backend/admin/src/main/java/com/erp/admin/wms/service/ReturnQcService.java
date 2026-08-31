package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.system.model.entity.SysFile;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.ReturnInboundMapper;
import com.erp.admin.wms.mapper.ReturnQcMapper;
import com.erp.admin.wms.mapper.WmsReturnQcItemMapper;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.model.dto.LocationInventoryKey;
import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.PutawayDTO;
import com.erp.admin.wms.model.dto.ReturnQcDTO;
import com.erp.admin.wms.model.dto.ReturnReceiveDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.entity.WmsReturnQcItem;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.enums.ReturnQcStatus;
import com.erp.admin.wms.model.qo.ReturnQO;
import com.erp.admin.wms.model.vo.ReturnOrderItemVO;
import com.erp.admin.wms.model.vo.ReturnOrderVO;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import com.erp.admin.wms.model.vo.WarehouseOptionVO;
import com.erp.admin.wms.model.vo.LocationCapacityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.ballcat.security.core.PrincipalAttributeAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 海外仓平台出库作业·退货质检（业务需求 1.4）。退货=带质检的重新入库。
 *
 * <p>基于既有 {@code wms_return_inbound_order}（附加 return_status 工作流）+ {@code wms_return_qc_item} 质检明细。
 * PASS→退货区 GOOD 生成新批次（新 inbound_date，FIFO 重排，可分配）；FAIL→不良品区 DAMAGED（不可分配）。
 * 批次生成复用 {@link WmsPhysicalInventoryService#putaway}（同事务聚合刷新 + 流水）。</p>
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnQcService {

    private static final List<String> ALL_SCOPE = Arrays.asList(
            ReturnQcStatus.RETURN_PENDING.name(),
            ReturnQcStatus.QC_PENDING.name(),
            ReturnQcStatus.COMPLETED.name(),
            ReturnQcStatus.CLOSED.name());

    private static final String ZONE_RETURN = "RETURN";
    private static final String ZONE_DEFECTIVE = "DEFECTIVE";
    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL";
    private static final Set<String> QC_PHOTO_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"));

    private final ReturnQcMapper returnQcMapper;

    private final ReturnInboundMapper returnInboundMapper;

    private final WmsReturnQcItemMapper returnQcItemMapper;

    private final WmsPhysicalInventoryService physicalInventoryService;

    private final LocationInventoryService locationInventoryService;

    private final LocationCapacityService locationCapacityService;

    private final WmsLocationMapper wmsLocationMapper;

    private final SkuMapper skuMapper;

    private final WmsLocationService wmsLocationService;

    private final WmsZoneService wmsZoneService;

    private final TenantIdentityService tenantIdentityService;

    private final com.erp.admin.order.mapper.ErpOrderItemMapper erpOrderItemMapper;

    private final com.erp.admin.order.mapper.ErpOrderMapper erpOrderMapper;

    private final com.erp.admin.tenant.mapper.SysTenantMapper sysTenantMapper;

    private final WmsRackAssignmentService wmsRackAssignmentService;

    private final WarehouseService warehouseService;

    private final WmsPalletService palletService;

    private final WmsSkuLookupMapper skuLookupMapper;

    private final SysFileService sysFileService;

    private final WarehouseSkuCodeService warehouseSkuCodeService;

    private final PrincipalAttributeAccessor principalAttributeAccessor;

    // ==================== 查询 ====================

    public PageResult<ReturnOrderVO> page(PageParam pageParam, ReturnQO qo) {
        assertPlatform();
        if (qo.getStatus() != null && !qo.getStatus().isEmpty()) {
            qo.setDbStatuses(java.util.Collections.singletonList(qo.getStatus()));
        } else {
            qo.setDbStatuses(ALL_SCOPE);
        }
        IPage<ReturnOrderVO> page = PageUtil.prodPage(pageParam);
        returnQcMapper.pageOrders(page, qo);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public ReturnOrderVO getDetail(Long id) {
        assertPlatform();
        ReturnOrderVO vo = returnQcMapper.selectOrderById(id);
        Assert.notNull(vo, "退货单不存在");
        vo.setItems(buildItems(id));
        return vo;
    }

    public List<PalletSummaryVO> listQcPallets(Long returnOrderId) {
        assertPlatform();
        ReturnInboundOrder order = returnInboundMapper.selectById(returnOrderId);
        Assert.notNull(order, "退货单不存在");
        Assert.isTrue(ReturnQcStatus.COMPLETED.name().equals(order.getReturnStatus()),
                "退货质检完成后才能打印托盘单");

        return Collections.emptyList();
    }

    // ==================== 收货 ====================

    @Transactional(rollbackFor = Exception.class)
    public void receive(ReturnReceiveDTO dto) {
        assertPlatform();
        ReturnInboundOrder order = returnInboundMapper.selectById(dto.getReturnOrderId());
        Assert.notNull(order, "退货单不存在");
        if (!ReturnQcStatus.RETURN_PENDING.name().equals(order.getReturnStatus())) {
            throw new BusinessException(400, "仅待退货收货的单据可以收货");
        }
        Assert.notEmpty(dto.getItems(), "收货明细不能为空");

        // 状态 CAS 抢占：RETURN_PENDING→QC_PENDING 原子推进，并发/双击收货只有一个赢家（消灭 TOCTOU，防重复建收货行）。
        int claimed = returnInboundMapper.casReturnStatus(order.getId(),
                ReturnQcStatus.RETURN_PENDING.name(), ReturnQcStatus.QC_PENDING.name());
        if (claimed != 1) {
            throw new BusinessException(400, "该退货单已收货或正在处理，请勿重复操作");
        }
		Assert.isTrue(returnInboundMapper.markReceivedAudit(order.getId(), currentUserId()) == 1,
				"退货收货操作记录失败");

        // 重置并按收货明细重建质检明细行（幂等：同单重复收货以最新为准）
        List<WmsReturnQcItem> existing = returnQcItemMapper.selectByReturnOrderId(order.getId());
        for (WmsReturnQcItem old : existing) {
            returnQcItemMapper.deleteById(old.getId());
        }
        // 应退数 = 退货单头件数（本系统退货单为单 SKU 单据）
        int expectedQty = nz(order.getTotalQuantity());
        Set<String> seenSku = new HashSet<>();
        for (ReturnReceiveDTO.ReceiveLine line : dto.getItems()) {
            // H-5 守恒守卫①：收货 SKU 必须属于本退货单（单 SKU 单据只应是单头 SKU），杜绝注入任意 SKU 幽灵库存
            Assert.isTrue(line.getSkuCode() != null && line.getSkuCode().equals(order.getSkuCode()),
                    "收货 SKU 不属于本退货单: " + line.getSkuCode());
            // 同一 SKU 不允许重复收货行
            Assert.isTrue(seenSku.add(line.getSkuCode()), "同一 SKU 收货行重复: " + line.getSkuCode());
            int received = nz(line.getReceivedQty());
            // Zero receipt is handled by the explicit close action and must not enter QC_PENDING.
            Assert.isTrue(received > 0, "实收数必须大于0；未收到货物请关闭退货单: " + line.getSkuCode());
            Assert.isTrue(received <= expectedQty,
                    String.format("实收数(%d)不能超过应退数(%d): %s", received, expectedQty, line.getSkuCode()));

            WmsReturnQcItem item = new WmsReturnQcItem();
            item.setReturnOrderId(order.getId());
            item.setSkuCode(line.getSkuCode());
            SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(order.getErpTenantId(), line.getSkuCode());
            item.setElectronic(sku != null && Boolean.TRUE.equals(sku.getNeedsPower()) ? 1 : 0);
            item.setExpectedQty(expectedQty);
            item.setReceivedQty(received);
            returnQcItemMapper.insert(item);
        }
        // 状态已在方法开头 CAS 抢占为 QC_PENDING，此处不再重复无条件写。
        log.info("退货收货完成, returnId={}, lines={}", order.getId(), dto.getItems().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void close(Long returnOrderId) {
        assertPlatform();
        Assert.notNull(returnInboundMapper.selectById(returnOrderId), "退货单不存在");
        int closed = returnInboundMapper.casReturnStatus(returnOrderId,
                ReturnQcStatus.RETURN_PENDING.name(), ReturnQcStatus.CLOSED.name());
        if (closed != 1) {
            throw new BusinessException(400, "仅待收货的退货单可以按未收到/拒收关闭");
        }
		Assert.isTrue(returnInboundMapper.markClosedAudit(returnOrderId, currentUserId()) == 1,
				"退货关闭操作记录失败");
        log.info("退货单按未收到/拒收关闭, returnId={}", returnOrderId);
    }

    // ==================== 质检 + 上架 ====================

    @Transactional(rollbackFor = Exception.class)
    public void qc(ReturnQcDTO dto) {
        assertPlatform();
        ReturnInboundOrder order = returnInboundMapper.selectById(dto.getReturnOrderId());
        Assert.notNull(order, "退货单不存在");
        if (!ReturnQcStatus.QC_PENDING.name().equals(order.getReturnStatus())) {
            throw new BusinessException(400, "仅待质检的单据可以质检");
        }
        Assert.notEmpty(dto.getLines(), "质检明细不能为空");

        Long targetWarehouseId = dto.getWarehouseId() == null ? order.getWarehouseId() : dto.getWarehouseId();
        validateAuthorizedWarehouse(order.getErpTenantId(), targetWarehouseId);
        order.setWarehouseId(targetWarehouseId);

        // 状态 CAS 抢占：QC_PENDING→COMPLETED 原子推进，并发/双击质检只有一个赢家（消灭 TOCTOU，防同一退货双份上架翻倍）。
        // 落败者在此即被挡下、不执行后续 putaway；本事务后续任何异常回滚也会一并撤销该状态推进。
        int claimed = returnInboundMapper.casReturnStatus(order.getId(),
                ReturnQcStatus.QC_PENDING.name(), ReturnQcStatus.COMPLETED.name());
        if (claimed != 1) {
            throw new BusinessException(400, "该退货单已质检或正在处理，请勿重复操作");
        }

        Map<String, WmsReturnQcItem> itemBySku = new HashMap<>();
        for (WmsReturnQcItem it : returnQcItemMapper.selectByReturnOrderId(order.getId())) {
            itemBySku.put(it.getSkuCode(), it);
        }
        // 分区类型 → zoneId
        Map<String, Long> zoneIdByType = new HashMap<>();
        for (WmsZone z : wmsZoneService.listByWarehouse(targetWarehouseId)) {
            zoneIdByType.putIfAbsent(z.getZoneType(), z.getId());
        }
        // 货架归属：只能上到本货主父服务商在本仓当前有效租用的货架排（与前端库位下拉同口径，防绕过直接提交）
        Set<String> allowedRacks = resolveAllowedRacks(order.getErpTenantId(), targetWarehouseId);

        int qualified = 0;
        int unqualified = 0;
        // H-4：同一 SKU 只能质检一行，防同 SKU 多行各自整批 putaway 致超量翻倍
        Set<String> judgedSku = new HashSet<>();
        for (ReturnQcDTO.ReturnQcLineDTO line : dto.getLines()) {
            WmsReturnQcItem item = itemBySku.get(line.getSkuCode());
            Assert.notNull(item, "质检明细缺少 SKU: " + line.getSkuCode());
            Assert.isTrue(judgedSku.add(line.getSkuCode()), "同一 SKU 不能重复质检: " + line.getSkuCode());
            int receivedQty = nz(item.getReceivedQty());
            int qualifiedQty = nz(line.getQualifiedQty());
            int damagedQty = nz(line.getDamagedQty());
            Assert.isTrue(receivedQty > 0, "实收数为0，无法上架: " + line.getSkuCode());
            Assert.isTrue(qualifiedQty >= 0 && damagedQty >= 0, "良品和残次品数量不能为负数");
            Assert.isTrue(qualifiedQty + damagedQty == receivedQty,
                    "良品数量与残次品数量之和必须等于实收数量: " + line.getSkuCode());

            validateQcPhotos(item, damagedQty, line.getPhotoFileIds());

            WmsLocation qualifiedLocation = null;
            WmsLocation damagedLocation = null;

            if (qualifiedQty > 0) {
                String zone = line.getQualifiedZone();
                Assert.isTrue(ZONE_RETURN.equals(zone), "退货质检良品只能上架到退货区");
                qualifiedLocation = putawayLogical(order, line.getSkuCode(), qualifiedQty, "GOOD",
                        line.getQualifiedLocationCode(), zone, allowedRacks);
            }
            if (damagedQty > 0) {
                damagedLocation = putawayLogical(order, line.getSkuCode(), damagedQty, "DEFECTIVE",
                        line.getDamagedLocationCode(), ZONE_DEFECTIVE, allowedRacks);
            }

            item.setQualifiedQty(qualifiedQty);
            item.setDamagedQty(damagedQty);
            item.setQualifiedZone(qualifiedQty > 0 ? line.getQualifiedZone() : null);
            item.setQualifiedLocationCode(qualifiedQty > 0 ? line.getQualifiedLocationCode() : null);
            item.setQualifiedLocationId(qualifiedLocation == null ? null : qualifiedLocation.getId());
            item.setQualifiedPalletId(null);
            item.setQualifiedSlotId(null);
            item.setQualifiedSlotCode(null);
            item.setDamagedLocationCode(damagedQty > 0 ? line.getDamagedLocationCode() : null);
            item.setDamagedLocationId(damagedLocation == null ? null : damagedLocation.getId());
            item.setDamagedPalletId(null);
            item.setDamagedSlotId(null);
            item.setDamagedSlotCode(null);
            item.setQcResult(qualifiedQty > 0 && damagedQty > 0 ? "MIXED" : qualifiedQty > 0 ? PASS : FAIL);
            item.setZone(qualifiedQty > 0 && damagedQty == 0 ? line.getQualifiedZone()
                    : damagedQty > 0 && qualifiedQty == 0 ? ZONE_DEFECTIVE : null);
            item.setQuality(qualifiedQty > 0 && damagedQty == 0 ? "GOOD"
                    : damagedQty > 0 && qualifiedQty == 0 ? "DEFECTIVE" : null);
            item.setLocationCode(qualifiedQty > 0 && damagedQty == 0 ? line.getQualifiedLocationCode()
                    : damagedQty > 0 && qualifiedQty == 0 ? line.getDamagedLocationCode() : null);
            item.setQcRemark(line.getQcRemark());
            item.setQcPhotos(joinPhotoIds(line.getPhotoFileIds()));
            Assert.isTrue(returnQcItemMapper.updateById(item) == 1, "质检明细更新失败");

            qualified += qualifiedQty;
            unqualified += damagedQty;
        }

        // M-5：每个已收货 SKU 必须被质检覆盖（判定 SKU 集合 == 已收货明细集合），
        // 防漏行货物永不入库、单据卡在 QC_PENDING 无法闭合。
        Assert.isTrue(judgedSku.size() == itemBySku.size(),
                "存在未质检的收货明细，请对全部收货 SKU 判定后再提交");

        order.setQualifiedQuantity(qualified);
        order.setUnqualifiedQuantity(unqualified);
        order.setReturnStatus(ReturnQcStatus.COMPLETED.name());
		order.setQcBy(currentUserId());
		order.setQcTime(LocalDateTime.now());
        returnInboundMapper.updateById(order);

        // 质检完成才真正扣减订单「已退数量」（申报期不扣，货主侧只申报）。按实收量=合格+不合格累加。
        int received = qualified + unqualified;
        if (received > 0 && order.getOrderItemId() != null) {
            // Platform users have tenant_id=-1. Temporarily switch to the return owner so the
            // tenant interceptor can update that owner's ERP order rows without weakening isolation.
            TenantContext.runAs(order.getErpTenantId(), () -> {
                int itemUpdated = erpOrderItemMapper.updateReturnedQuantity(order.getOrderItemId(), received);
                Assert.isTrue(itemUpdated == 1, "订单明细已退数量更新失败或超过原出库数量");
                if (order.getErpOrderId() != null) {
                    int orderUpdated = erpOrderMapper.updateReturnedQuantity(order.getErpOrderId(), received);
                    Assert.isTrue(orderUpdated == 1, "订单已退数量更新失败或超过订单总数量");
                }
                return null;
            });
        } else if (order.getOrderItemId() == null) {
            log.warn("退货单 {} 缺 orderItemId（历史申报），跳过已退数量回写", order.getId());
        }
        log.info("退货质检完成, returnId={}, qualified={}, unqualified={}, 回写已退数量={}",
                order.getId(), qualified, unqualified, received);
    }

	private Long currentUserId() {
		Long userId = principalAttributeAccessor.getUserId();
		Assert.notNull(userId, "无法获取当前操作人");
		return userId;
	}

    private void validateQcPhotos(WmsReturnQcItem item, int damagedQty, List<Long> fileIds) {
        List<Long> ids = fileIds == null ? Collections.emptyList() : fileIds.stream()
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        Assert.isTrue(ids.size() <= 6, "每个 SKU 最多上传6张质检照片");
        if (damagedQty > 0 && Integer.valueOf(1).equals(item.getElectronic())) {
            Assert.notEmpty(ids, "电子类商品存在残次品时必须上传质检照片: " + item.getSkuCode());
        }
        for (Long fileId : ids) {
            SysFile file = sysFileService.getById(fileId);
            Assert.notNull(file, "质检照片不存在: " + fileId);
            Assert.isTrue(file.getContentType() != null
                            && QC_PHOTO_TYPES.contains(file.getContentType().toLowerCase()),
                    "质检照片仅支持 JPG、JPEG、PNG 格式");
        }
    }

    private WmsLocation putawayLogical(ReturnInboundOrder order, String skuCode, int quantity,
            String quality, String locationCode, String requiredZoneType, Set<String> allowedRacks) {
        Assert.hasText(locationCode, "请选择" + (ZONE_DEFECTIVE.equals(requiredZoneType) ? "不良品" : "退货") + "库位");
        WmsLocation selected = wmsLocationService.listByWarehouse(order.getWarehouseId()).stream()
                .filter(location -> locationCode.equals(location.getLocationCode()))
                .findFirst().orElse(null);
        Assert.notNull(selected, "目标库位不存在于当前仓库：" + locationCode);
        WmsLocation locked = wmsLocationMapper.selectLogicalByIdForUpdate(selected.getId());
        Assert.notNull(locked, "目标库位已被删除，请刷新后重试");
        WmsZone zone = wmsZoneService.getById(locked.getZoneId());
        Assert.isTrue(zone != null && requiredZoneType.equals(zone.getZoneType()),
                "目标库位不属于" + (ZONE_DEFECTIVE.equals(requiredZoneType) ? "不良品区" : "退货区"));
        Assert.isTrue(allowedRacks.contains(locked.getRackNo()), "目标库位不属于当前货主服务商：" + locationCode);

        Sku sku = TenantContext.runAs(order.getErpTenantId(), () -> skuMapper.selectBySkuCode(skuCode));
        Assert.notNull(sku, "SKU不存在：" + skuCode);
        LocationCapacityVO capacity = TenantContext.runAs(order.getErpTenantId(), () -> locationCapacityService.evaluate(
                locked.getId(), Collections.singletonList(LocationCapacityService.fromSku(sku, quantity))));
        Assert.isTrue(capacity.isVolumeAllowed(), "目标库位空间不足：" + locationCode);
        Assert.isTrue(capacity.isWeightAllowed(), "目标库位承重不足：" + locationCode);
        Assert.isTrue(capacity.isSkuKindsAllowed(), "目标库位SKU种类数超限：" + locationCode);

        com.erp.admin.tenant.model.entity.SysTenant owner = sysTenantMapper.selectById(order.getErpTenantId());
        Assert.isTrue(owner != null && owner.getParentWmsTenantId() != null, "货主未绑定WMS服务商");
        LocationInventoryKey key = new LocationInventoryKey();
        key.setTenantId(TenantContext.BLOCK_TENANT_ID);
        key.setWmsTenantId(owner.getParentWmsTenantId());
        key.setErpTenantId(order.getErpTenantId());
        key.setWarehouseId(order.getWarehouseId());
        key.setLocationId(locked.getId());
        key.setSkuCode(skuCode);
        key.setQuality(quality);
        locationInventoryService.increase(key, quantity,
				com.erp.admin.wms.model.dto.InventoryMutationContext.builder()
						.eventType(com.erp.admin.wms.model.enums.InventoryEventType.RETURN_PUTAWAY)
						.sourceType("RETURN_QC").sourceId(order.getId()).sourceNo(order.getReturnNo())
						.operatorId(currentUserId()).reason("退货质检上架")
						.idempotencyKey("return-putaway:" + order.getId() + ":" + locked.getId()
								+ ":" + skuCode + ":" + quality).build());
        return locked;
    }

    private PalletPlacement putawayOnPallet(ReturnInboundOrder order, String skuCode, int quantity,
            String quality, String zoneType, Long zoneId, String slotCode, Long requestedPalletId,
            java.math.BigDecimal capacityPercent, int allocatable, Set<String> allowedRacks,
            Set<String> usedSlots) {
        Assert.notNull(zoneId, "该仓库无对应分区: " + zoneType);
        Assert.notNull(capacityPercent, "请填写托盘容量");
        Assert.isTrue(capacityPercent.compareTo(java.math.BigDecimal.ZERO) > 0
                        && capacityPercent.compareTo(new java.math.BigDecimal("100")) <= 0,
                "托盘容量必须在1%到100%之间");
        Assert.isTrue(usedSlots.add(slotCode), "同一次质检不能重复选择同一层位: " + slotCode);

        PalletSlotVO slot = palletService.listSlots(order.getWarehouseId()).stream()
                .filter(value -> slotCode.equals(value.getSlotCode())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("托盘层位不存在: " + slotCode));
        Assert.isTrue(allowedRacks.contains(slot.getRackNo()), "托盘层位不在本货主可用货架排: " + slotCode);
        Assert.isTrue(zoneType.equals(slot.getZoneType()), "托盘层位不属于所选分区: " + slotCode);

        InboundPutawayDTO.PutawayLine palletLine = new InboundPutawayDTO.PutawayLine();
        palletLine.setSkuCode(skuCode);
        palletLine.setLocationCode(slot.getLocationCode());
        palletLine.setQuantity(quantity);
        palletLine.setQuality(quality);
        palletLine.setSlotCode(slotCode);
        palletLine.setPalletId(requestedPalletId);
        palletLine.setPalletKey("RETURN-" + order.getId() + "-" + quality + "-" + skuCode);
        palletLine.setCapacityPercent(capacityPercent);
        palletLine.setCapacitySource("MANUAL");
        palletLine.setManualFull(capacityPercent.compareTo(new java.math.BigDecimal("100")) >= 0);

        WmsPallet pallet;
        if (requestedPalletId == null) {
            Assert.isTrue(WmsPalletService.EMPTY.equals(slot.getSlotStatus()), "所选层位已被占用: " + slotCode);
            pallet = palletService.createForPutaway(order.getWarehouseId(), order.getErpTenantId(),
                    slotCode, Collections.singletonList(palletLine));
        } else {
            Assert.isTrue(requestedPalletId.equals(slot.getPalletId()), "所选托盘已不在该层位，请刷新后重试");
            Assert.isTrue(slot.getCapacityPercent() == null
                            || capacityPercent.compareTo(slot.getCapacityPercent()) >= 0,
                    "合并后的托盘容量不能小于当前容量");
            PalletSummaryVO current = palletService.getDetail(requestedPalletId);
            Assert.isTrue(current.getItems().stream().allMatch(value -> quality.equals(value.getQuality())),
                    "不同品质的货物不能混放在同一托盘");
            pallet = palletService.lockExistingForPutaway(requestedPalletId, order.getErpTenantId(),
                    Collections.singletonList(palletLine));
        }

        PutawayDTO put = new PutawayDTO();
        // Physical inventory resolves the owner's WMS provider centrally.
        put.setWmsTenantId(null);
        put.setErpTenantId(order.getErpTenantId());
        put.setWarehouseId(order.getWarehouseId());
        put.setSkuCode(skuCode);
        put.setInboundItemId(0L);
        put.setQuantity(quantity);
        put.setQuality(quality);
        put.setLocationCode(slot.getLocationCode());
        put.setPalletId(pallet.getId());
        put.setSlotId(slot.getSlotId());
        put.setZoneId(zoneId);
        put.setAllocatable(allocatable);
        physicalInventoryService.putaway(put);
        palletService.refreshAfterInventoryChange(pallet.getId());
        return new PalletPlacement(pallet.getId(), slot.getSlotId(), slot.getSlotCode(), slot.getLocationCode());
    }

    private void validateLocation(String locationCode, String zone, Long zoneId,
                                  Map<String, WmsLocation> locByCode, Set<String> occupied,
                                  Set<String> usedInThisSubmit, Set<String> allowedRacks) {
        Assert.hasText(locationCode, "请填写回库库位");
        Assert.notNull(zoneId, "该仓库无对应分区: " + zone);
        WmsLocation loc = locByCode.get(locationCode);
        Assert.notNull(loc, "回库库位不存在于本仓: " + locationCode);
        if (occupied.contains(locationCode) || !usedInThisSubmit.add(locationCode)) {
            throw new BusinessException(400, "回库库位已被占用: " + locationCode);
        }
        Assert.isTrue(zoneId.equals(loc.getZoneId()),
                "回库库位不属于所选分区(" + zone + "): " + locationCode);
        Assert.isTrue(allowedRacks.contains(loc.getRackNo()),
                "回库库位不在本货主可用货架排: " + locationCode);
    }

    private void putaway(ReturnInboundOrder order, String skuCode, int quantity, String quality,
                         String locationCode, Long zoneId, int allocatable) {
        PutawayDTO put = new PutawayDTO();
        // Physical inventory resolves the owner's WMS provider centrally.
        put.setWmsTenantId(null);
        put.setErpTenantId(order.getErpTenantId());
        put.setWarehouseId(order.getWarehouseId());
        put.setSkuCode(skuCode);
        put.setInboundItemId(0L);
        put.setQuantity(quantity);
        put.setQuality(quality);
        put.setLocationCode(locationCode);
        put.setZoneId(zoneId);
        put.setAllocatable(allocatable);
        physicalInventoryService.putaway(put);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class PalletPlacement {
        private final Long palletId;
        private final Long slotId;
        private final String slotCode;
        private final String locationCode;

        private PalletPlacement(Long palletId, Long slotId, String slotCode, String locationCode) {
            this.palletId = palletId;
            this.slotId = slotId;
            this.slotCode = slotCode;
            this.locationCode = locationCode;
        }
    }

    // ==================== 可用库位 ====================

    public List<WarehouseOptionVO> listAuthorizedWarehouses(Long returnOrderId) {
        assertPlatform();
        ReturnInboundOrder order = returnInboundMapper.selectById(returnOrderId);
        Assert.notNull(order, "退货单不存在");
        return warehouseService.getWarehouseOptions().stream()
                .filter(option -> "OWN".equals(option.getWarehouseType()))
                .filter(option -> !resolveAllowedRacks(order.getErpTenantId(), option.getId()).isEmpty())
                .collect(Collectors.toList());
    }

    public List<PalletSlotVO> listAvailableSlots(Long returnOrderId, Long warehouseId, String zoneType) {
        assertPlatform();
        ReturnInboundOrder order = returnInboundMapper.selectById(returnOrderId);
        Assert.notNull(order, "退货单不存在");
        Assert.isTrue(ZONE_RETURN.equals(zoneType) || ZONE_DEFECTIVE.equals(zoneType),
                "退货质检只支持退货区或不良品区");
        validateAuthorizedWarehouse(order.getErpTenantId(), warehouseId);

        Set<String> allowedRacks = resolveAllowedRacks(order.getErpTenantId(), warehouseId);
        Warehouse warehouse = warehouseService.getById(warehouseId);
        String quality = ZONE_DEFECTIVE.equals(zoneType) ? "DAMAGED" : "GOOD";
        Map<Long, PalletSummaryVO> partialPallets = palletService
                .listPallets(warehouseId, null, null, WmsPalletService.PARTIAL).stream()
                .collect(Collectors.toMap(PalletSummaryVO::getId, value -> value, (a, b) -> a));

        List<PalletSlotVO> result = palletService.listSlots(warehouseId).stream()
                .filter(slot -> zoneType.equals(slot.getZoneType()))
                .filter(slot -> allowedRacks.contains(slot.getRackNo()))
                .filter(slot -> WmsPalletService.EMPTY.equals(slot.getSlotStatus())
                        || canMergeReturn(slot, partialPallets.get(slot.getPalletId()), warehouse,
                                order.getErpTenantId(), order.getSkuCode(), quality))
                .collect(Collectors.toList());
        result.sort(Comparator
                .comparingInt((PalletSlotVO slot) -> returnSlotRank(slot,
                        partialPallets.get(slot.getPalletId()), order.getErpTenantId(), order.getSkuCode()))
                .thenComparing(PalletSlotVO::getLevelNo)
                .thenComparing(PalletSlotVO::getSlotCode));
        return result;
    }

    private boolean canMergeReturn(PalletSlotVO slot, PalletSummaryVO pallet, Warehouse warehouse,
            Long ownerId, String skuCode, String quality) {
        if (pallet == null || !WmsPalletService.PARTIAL.equals(slot.getPalletStatus())
                || slot.getCapacityPercent() == null
                || slot.getCapacityPercent().compareTo(new java.math.BigDecimal("100")) >= 0
                || pallet.getItems() == null || pallet.getItems().isEmpty()
                || pallet.getItems().stream().anyMatch(item -> !quality.equals(item.getQuality()))) {
            return false;
        }
        boolean crossOwner = warehouse.getAllowCrossOwnerMix() == null
                || Integer.valueOf(1).equals(warehouse.getAllowCrossOwnerMix());
        if (!crossOwner && pallet.getItems().stream().anyMatch(item -> !ownerId.equals(item.getErpTenantId()))) {
            return false;
        }
        Set<String> kinds = pallet.getItems().stream()
                .map(item -> item.getErpTenantId() + "|" + item.getSkuCode())
                .collect(Collectors.toSet());
        kinds.add(ownerId + "|" + skuCode);
        int maxKinds = warehouse.getMaxSkuKindsPerPallet() == null ? 4 : warehouse.getMaxSkuKindsPerPallet();
        return kinds.size() <= maxKinds;
    }

    private int returnSlotRank(PalletSlotVO slot, PalletSummaryVO pallet, Long ownerId, String skuCode) {
        if (pallet != null && pallet.getItems() != null && pallet.getItems().stream()
                .allMatch(item -> ownerId.equals(item.getErpTenantId()) && skuCode.equals(item.getSkuCode()))) {
            return 0;
        }
        return pallet == null ? 2 : 1;
    }

    private void validateAuthorizedWarehouse(Long erpTenantId, Long warehouseId) {
        Assert.notNull(warehouseId, "请选择退货仓库");
        Warehouse warehouse = warehouseService.getById(warehouseId);
        Assert.notNull(warehouse, "退货仓库不存在");
        Assert.isTrue(Integer.valueOf(1).equals(warehouse.getStatus()), "退货仓库未启用");
        Assert.isTrue("OWN".equals(warehouse.getWarehouseType()), "退货质检只能进入自有仓");
        Assert.isTrue(!resolveAllowedRacks(erpTenantId, warehouseId).isEmpty(),
                "当前货主无权使用所选退货仓库");
    }

    /**
     * 按「退货单所属仓库」+分区列出「未被占用」的可选库位（供质检上架下拉）。
     * <p>仓库不由前端传入，而是从退货单取（该退货单所属货主申报时选定的仓库），
     * 保证平台操作员只能选到本单对应仓库的库位，不能越权到其它仓库/货主。
     * 质检口径：PASS→退货区(RETURN)，FAIL→次品区(DEFECTIVE)；仅平台身份可查。</p>
     */
    public List<String> listAvailableLocations(Long returnOrderId, Long selectedWarehouseId, String zoneType) {
        assertPlatform();
        Assert.notNull(returnOrderId, "退货单ID不能为空");
        Assert.hasText(zoneType, "分区不能为空");
        ReturnInboundOrder order = returnInboundMapper.selectById(returnOrderId);
        Assert.notNull(order, "退货单不存在");
        Long warehouseId = selectedWarehouseId == null ? order.getWarehouseId() : selectedWarehouseId;
        Assert.notNull(warehouseId, "退货单未指定仓库");
        validateAuthorizedWarehouse(order.getErpTenantId(), warehouseId);

        // 货架归属：只允许上到「本退货单货主的父服务商」在本仓当前有效租用的货架排上（与入库上架同口径）。
        // 任一环缺失 → 无可上架货架 → 空列表。
        Set<String> allowedRacks = resolveAllowedRacks(order.getErpTenantId(), warehouseId);
        if (allowedRacks.isEmpty()) {
            return new ArrayList<>();
        }

        Long zoneId = null;
        for (WmsZone z : wmsZoneService.listByWarehouse(warehouseId)) {
            if (zoneType.equals(z.getZoneType())) {
                zoneId = z.getId();
                break;
            }
        }
        if (zoneId == null) {
            return new ArrayList<>();
        }
        final Long zid = zoneId;
        return wmsLocationService.listByWarehouse(warehouseId).stream()
                .filter(l -> zid.equals(l.getZoneId()))
                .filter(l -> allowedRacks.contains(l.getRackNo()))
                .map(WmsLocation::getLocationCode)
                .filter(code -> code != null && !code.isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 该退货单可上架的货架排：货主(erp_tenant_id) → 父服务商(parent_wms_tenant_id) →
     * 该仓当前有效租用的货架排号。任一环缺失返回空集。
     */
    private Set<String> resolveAllowedRacks(Long erpTenantId, Long warehouseId) {
        if (erpTenantId == null) {
            return new HashSet<>();
        }
        com.erp.admin.tenant.model.entity.SysTenant owner = sysTenantMapper.selectById(erpTenantId);
        Long operatorId = owner == null ? null : owner.getParentWmsTenantId();
        if (operatorId == null) {
            return new HashSet<>();
        }
        return new HashSet<>(wmsRackAssignmentService.activeRackNos(warehouseId, operatorId));
    }

    // ==================== 组装 / 辅助 ====================

    private List<ReturnOrderItemVO> buildItems(Long orderId) {
        List<WmsReturnQcItem> items = returnQcItemMapper.selectByReturnOrderId(orderId);
        List<ReturnOrderItemVO> result = new ArrayList<>();
        ReturnInboundOrder order = returnInboundMapper.selectById(orderId);
        if (!items.isEmpty()) {
            for (WmsReturnQcItem it : items) {
                ReturnOrderItemVO vo = new ReturnOrderItemVO();
                vo.setSkuCode(it.getSkuCode());
                vo.setWarehouseSkuCode(warehouseSkuCodeService.build(
                        order.getErpTenantId(), it.getSkuCode()));
                SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(
                        order == null ? null : order.getErpTenantId(), it.getSkuCode());
                vo.setElectronic(it.getElectronic() != null && it.getElectronic() == 1
                        || sku != null && Boolean.TRUE.equals(sku.getNeedsPower()));
                vo.setQuantityPerPallet(sku == null ? null : sku.getQuantityPerPallet());
                vo.setSkuName(sku == null ? null : sku.getChineseName());
                vo.setExpectedQty(it.getExpectedQty());
                vo.setReceivedQty(it.getReceivedQty());
                vo.setQualifiedQty(it.getQualifiedQty());
                vo.setDamagedQty(it.getDamagedQty());
                vo.setQualifiedZone(it.getQualifiedZone());
                vo.setQualifiedLocationCode(it.getQualifiedLocationCode());
                vo.setQualifiedPalletId(it.getQualifiedPalletId());
                vo.setQualifiedSlotId(it.getQualifiedSlotId());
                vo.setQualifiedSlotCode(it.getQualifiedSlotCode());
                vo.setDamagedLocationCode(it.getDamagedLocationCode());
                vo.setDamagedPalletId(it.getDamagedPalletId());
                vo.setDamagedSlotId(it.getDamagedSlotId());
                vo.setDamagedSlotCode(it.getDamagedSlotCode());
                vo.setQcResult(it.getQcResult());
                vo.setZone(it.getZone());
                vo.setQuality(it.getQuality());
                vo.setLocationCode(it.getLocationCode());
                vo.setQcRemark(it.getQcRemark());
                vo.setQcPhotoFileIds(parsePhotoIds(it.getQcPhotos()));
                result.add(vo);
            }
            return result;
        }
        // 尚未收货：由退货单头合成单一明细（单SKU扁平结构）
        if (order != null && order.getSkuCode() != null) {
            ReturnOrderItemVO vo = new ReturnOrderItemVO();
            vo.setSkuCode(order.getSkuCode());
            vo.setWarehouseSkuCode(warehouseSkuCodeService.build(
                    order.getErpTenantId(), order.getSkuCode()));
            SkuLookupVO sku = skuLookupMapper.findByTenantAndSku(order.getErpTenantId(), order.getSkuCode());
            vo.setElectronic(sku != null && Boolean.TRUE.equals(sku.getNeedsPower()));
            vo.setQuantityPerPallet(sku == null ? null : sku.getQuantityPerPallet());
            vo.setSkuName(sku == null ? null : sku.getChineseName());
            vo.setExpectedQty(order.getTotalQuantity());
            result.add(vo);
        }
        return result;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 质检照片文件ID列表 → CSV 存库（null/空返回 null，清空照片） */
    private static String joinPhotoIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /** CSV → 质检照片文件ID列表，供只读回显 */
    private static List<Long> parsePhotoIds(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return ids;
        }
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(t));
            } catch (NumberFormatException ignore) {
                // 脏数据跳过，不影响回显
            }
        }
        return ids;
    }

    private void assertPlatform() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
            throw new BusinessException(403, "仅海外仓平台可执行退货质检");
        }
    }

}
