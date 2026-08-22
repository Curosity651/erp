package com.erp.admin.wms.service;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.mapper.WmsLocationSlotMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsPalletMapper;
import com.erp.admin.wms.mapper.WmsPalletOperationLogMapper;
import com.erp.admin.wms.mapper.WmsPhysicalInventoryMapper;
import com.erp.admin.wms.model.dto.InboundPutawayDTO;
import com.erp.admin.wms.model.dto.PalletCapacityDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.entity.WmsLocationSlot;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.entity.WmsPalletOperationLog;
import com.erp.admin.wms.model.entity.WmsPhysicalInventory;
import com.erp.admin.wms.model.entity.WmsZone;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import com.erp.admin.wms.model.vo.LocationSlotLevelSummaryVO;
import com.erp.admin.wms.model.vo.LocationSlotSummaryVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Physical pallet lifecycle. Inventory quantities remain in wms_physical_inventory;
 * this service only owns placement, capacity and pallet state.
 */
@Service
@RequiredArgsConstructor
public class WmsPalletService {

    public static final String EMPTY = "EMPTY";
    public static final String OCCUPIED = "OCCUPIED";
    public static final String PARTIAL = "PARTIAL";
    public static final String FULL = "FULL";
    public static final String CLOSED = "CLOSED";

    private final WmsLocationSlotMapper slotMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsPalletMapper palletMapper;
    private final WmsPalletOperationLogMapper operationLogMapper;
    private final WmsPhysicalInventoryMapper physicalInventoryMapper;
    private final WmsLocationService locationService;
    private final WmsZoneService zoneService;
    private final WarehouseMapper warehouseMapper;
    private final SysTenantMapper tenantMapper;
    private final WarehouseSkuCodeService warehouseSkuCodeService;
    private final PrincipalAttributeAccessor principalAttributeAccessor;

    public WmsPallet getById(Long id) {
        return palletMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int ensureSlots(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        List<WmsLocation> locations = physicalLocations(warehouseId);
        return ensureSlots(warehouseId, warehouse, locations);
    }

    private List<WmsLocation> physicalLocations(Long warehouseId) {
        return locationService.listPhysicalByWarehouse(warehouseId);
    }

    public int deletePhysicalSlots(Long warehouseId) {
        return slotMapper.deletePhysicalSlots(warehouseId);
    }

    public int countPhysicalSlots(Long warehouseId) {
        return slotMapper.countPhysicalSlots(warehouseId);
    }

    private int ensureSlots(Long warehouseId, Warehouse warehouse, List<WmsLocation> locations) {
        int levels = warehouse.getPalletLevels() == null ? 6 : warehouse.getPalletLevels();
        levels = Math.max(1, Math.min(levels, 12));
        int positions = warehouse.getPalletPositionsPerLevel() == null
                ? 1 : warehouse.getPalletPositionsPerLevel();
        positions = Math.max(1, Math.min(positions, 9));
        long expectedCount = (long) locations.size() * levels * positions;
        Long actualCount = slotMapper.selectCount(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId));
        if (actualCount != null && actualCount.longValue() == expectedCount) {
            return 0;
        }
        Set<String> existing = slotMapper.selectList(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId)).stream()
                .map(slot -> slot.getLocationId() + "|" + slot.getLevelNo() + "|" + value(slot.getPositionNo()))
                .collect(Collectors.toSet());
        List<WmsLocationSlot> pending = new ArrayList<>();
        for (WmsLocation location : locations) {
            for (int level = 1; level <= levels; level++) {
                for (int position = 1; position <= positions; position++) {
                    if (existing.contains(location.getId() + "|" + level + "|" + position)) {
                        continue;
                    }
                    WmsLocationSlot slot = new WmsLocationSlot();
                    slot.setWarehouseId(warehouseId);
                    slot.setLocationId(location.getId());
                    slot.setLevelNo(level);
                    slot.setPositionNo(position);
                    slot.setSlotCode(location.getLocationCode() + "-L" + level
                            + "-P" + String.format("%02d", position));
                    slot.setMaxHeightMm(warehouse.getDefaultPalletHeightMm());
                    slot.setMaxWeightKg(warehouse.getDefaultPalletMaxWeightKg());
                    slot.setSlotStatus(EMPTY);
                    slot.setVersion(0);
                    pending.add(slot);
                    if (pending.size() >= 500) {
                        slotMapper.insertBatch(pending);
                        pending.clear();
                    }
                }
            }
        }
        if (!pending.isEmpty()) {
            slotMapper.insertBatch(pending);
        }
        long after = slotMapper.selectCount(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId));
        return Math.toIntExact(Math.max(0L, after - (actualCount == null ? 0L : actualCount)));
    }

    public List<LocationSlotLevelSummaryVO> listSlotLevelSummaries(Long warehouseId) {
        return slotMapper.listLevelSummaries(warehouseId);
    }

    public List<LocationSlotSummaryVO> listSlotSummaries(Long warehouseId) {
        Map<Long, LocationSlotSummaryVO> summaries = new LinkedHashMap<>();
        for (LocationSlotLevelSummaryVO level : listSlotLevelSummaries(warehouseId)) {
            LocationSlotSummaryVO summary = summaries.computeIfAbsent(level.getLocationId(), locationId -> {
                LocationSlotSummaryVO item = new LocationSlotSummaryVO();
                item.setLocationId(locationId);
                return item;
            });
            summary.setTotalSlots(value(summary.getTotalSlots()) + value(level.getTotalCount()));
            summary.setOccupiedSlots(value(summary.getOccupiedSlots()) + value(level.getOccupiedCount()));
            summary.setBlocked(Boolean.TRUE.equals(summary.getBlocked()) || value(level.getBlocked()) > 0);
            summary.getLevels().add(level);
        }
        return new ArrayList<>(summaries.values());
    }

    public List<PalletSlotVO> listSlots(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        List<WmsLocation> locations = physicalLocations(warehouseId);
        Map<Long, WmsLocation> locationById = locations.stream()
                .collect(Collectors.toMap(WmsLocation::getId, value -> value, (a, b) -> a));
        Map<Long, WmsZone> zoneById = zoneService.listByWarehouse(warehouseId).stream()
                .collect(Collectors.toMap(WmsZone::getId, value -> value, (a, b) -> a));
        Map<Long, WmsPallet> palletBySlot = palletMapper.selectList(WrappersX.lambdaQueryX(WmsPallet.class)
                .eq(WmsPallet::getWarehouseId, warehouseId)
                .isNotNull(WmsPallet::getCurrentSlotId)).stream()
                .collect(Collectors.toMap(WmsPallet::getCurrentSlotId, value -> value, (a, b) -> a));
        List<PalletSlotVO> result = new ArrayList<>();
        for (WmsLocationSlot slot : slotMapper.selectList(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId)
                .orderByAsc(WmsLocationSlot::getSlotCode))) {
            WmsLocation location = locationById.get(slot.getLocationId());
            if (location == null) {
                continue;
            }
            WmsZone zone = location.getZoneId() == null ? null : zoneById.get(location.getZoneId());
            WmsPallet pallet = palletBySlot.get(slot.getId());
            PalletSlotVO vo = new PalletSlotVO();
            vo.setSlotId(slot.getId());
            vo.setLocationId(location.getId());
            vo.setLocationCode(location.getLocationCode());
            vo.setSlotCode(slot.getSlotCode());
            vo.setRackNo(location.getRackNo());
            vo.setColumnNo(location.getColumnNo());
            vo.setLevelNo(slot.getLevelNo());
            vo.setPositionNo(slot.getPositionNo());
            vo.setZoneId(location.getZoneId());
            vo.setZoneName(zone == null ? null : zone.getZoneName());
            vo.setZoneType(zone == null ? null : zone.getZoneType());
            vo.setSlotStatus(slot.getSlotStatus());
            vo.setMaxWeightKg(slot.getMaxWeightKg());
            if (pallet != null) {
                vo.setPalletId(pallet.getId());
                vo.setPalletNo(pallet.getPalletNo());
                vo.setPalletType(pallet.getPalletType());
                vo.setPalletStatus(pallet.getPalletStatus());
                vo.setCapacityPercent(pallet.getCapacityPercent());
                vo.setSkuKindCount(pallet.getSkuKindCount());
            }
            result.add(vo);
        }
        result.sort(Comparator.comparing(PalletSlotVO::getRackNo, Comparator.nullsLast(String::compareTo))
                .thenComparing(PalletSlotVO::getColumnNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PalletSlotVO::getLevelNo)
                .thenComparing(PalletSlotVO::getPositionNo, Comparator.nullsLast(Integer::compareTo)));
        return result;
    }

    public WmsLocationSlot requireSlot(Long warehouseId, String slotCode) {
        WmsLocationSlot slot = slotMapper.selectOne(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId)
                .eq(WmsLocationSlot::getSlotCode, slotCode));
        Assert.notNull(slot, "层位不存在：" + slotCode);
        return slot;
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsPallet createForPutaway(Long warehouseId, Long erpTenantId, String slotCode,
            List<InboundPutawayDTO.PutawayLine> lines) {
        Assert.notEmpty(lines, "托盘明细不能为空");
        Assert.notNull(erpTenantId, "托盘必须指定货主");
        WmsLocationSlot slot = requireSlot(warehouseId, slotCode);
        WmsLocation location = locationMapper.selectByIdForUpdate(slot.getLocationId());
        Assert.isTrue(location != null && warehouseId.equals(location.getWarehouseId())
                && !Integer.valueOf(1).equals(location.getIsVirtual()), "目标物理库位不存在");
        if (slotMapper.claim(slot.getId()) != 1) {
            throw new BusinessException(409, "层位已被其他托盘占用：" + slotCode);
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        validateCapacityAndMixing(warehouse, erpTenantId, Collections.emptyList(), lines, slot);

        Set<String> kinds = lines.stream().map(line -> erpTenantId + "|" + line.getSkuCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        BigDecimal capacity = lines.stream().map(InboundPutawayDTO.PutawayLine::getCapacityPercent)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(null);
        boolean manualFull = lines.stream().anyMatch(line -> Boolean.TRUE.equals(line.getManualFull()));
        boolean full = manualFull || (capacity != null && capacity.compareTo(new BigDecimal("100")) >= 0);
        WmsPallet pallet = new WmsPallet();
        pallet.setPalletNo(nextPalletNo());
        pallet.setWarehouseId(warehouseId);
        pallet.setErpTenantId(erpTenantId);
        SysTenant owner = tenantMapper.selectById(erpTenantId);
        pallet.setWmsTenantId(owner == null ? null : owner.getParentWmsTenantId());
        pallet.setSlotId(slot.getId());
        pallet.setCurrentSlotId(slot.getId());
        pallet.setSlotCode(slot.getSlotCode());
        pallet.setPalletType(kinds.size() > 1 ? "MIXED" : full ? "SINGLE_FULL" : "SINGLE_PARTIAL");
        pallet.setPalletStatus(full ? FULL : PARTIAL);
        pallet.setCapacityPercent(capacity);
        pallet.setCapacitySource(firstNonBlank(lines.get(0).getCapacitySource(), "MANUAL_REQUIRED"));
        pallet.setActualWeightKg(lines.stream().map(InboundPutawayDTO.PutawayLine::getActualWeightKg)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(null));
        pallet.setSkuKindCount(kinds.size());
        pallet.setWholePalletEligible(full && kinds.size() == 1 ? 1 : 0);
        pallet.setManualFull(manualFull ? 1 : 0);
        pallet.setLabelVersion(1);
        pallet.setVersion(0);
        pallet.setCreateBy(currentUserId());
        pallet.setUpdateBy(currentUserId());
        palletMapper.insert(pallet);
        writeLog(pallet, "CREATE", null, slotCode, "Inbound putaway");
        return pallet;
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsPallet lockExistingForPutaway(Long palletId, Long erpTenantId,
            List<InboundPutawayDTO.PutawayLine> lines) {
        WmsPallet pallet = palletMapper.selectForUpdate(palletId);
        Assert.notNull(pallet, "托盘不存在");
        Assert.isTrue(pallet.getErpTenantId() == null || erpTenantId.equals(pallet.getErpTenantId()),
                "托盘属于其他货主，禁止混托");
        Assert.isTrue(pallet.getCurrentSlotId() != null, "托盘已离开仓库，不能继续合并");
        Assert.isTrue(PARTIAL.equals(pallet.getPalletStatus()), "只有未满托盘可以继续合并");
        WmsLocationSlot slot = slotMapper.selectForUpdate(pallet.getCurrentSlotId());
        WmsLocation location = locationMapper.selectByIdForUpdate(slot.getLocationId());
        Assert.isTrue(location != null && pallet.getWarehouseId().equals(location.getWarehouseId())
                && !Integer.valueOf(1).equals(location.getIsVirtual()), "目标物理库位不存在");
        Warehouse warehouse = warehouseMapper.selectById(pallet.getWarehouseId());
        List<WmsPhysicalInventory> current = physicalInventoryMapper.selectList(
                WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
                        .eq(WmsPhysicalInventory::getPalletId, palletId)
                        .gt(WmsPhysicalInventory::getQuantity, 0));
        String incomingQuality = normalizeQuality(lines.get(0).getQuality());
        Assert.isTrue(current.stream().allMatch(item ->
                        incomingQuality.equals(normalizeQuality(item.getQuality()))),
                "同一托盘不能混放良品和残次品");
        validateCapacityAndMixing(warehouse, erpTenantId, current, lines, slot);
        BigDecimal submitted = lines.stream().map(InboundPutawayDTO.PutawayLine::getCapacityPercent)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(null);
        if (submitted != null) {
            pallet.setCapacityPercent(submitted);
            pallet.setCapacitySource(firstNonBlank(lines.get(0).getCapacitySource(), pallet.getCapacitySource()));
        }
        else {
            pallet.setCapacityPercent(null);
            pallet.setCapacitySource("MANUAL_REQUIRED");
            pallet.setWholePalletEligible(0);
        }
        pallet.setManualFull(lines.stream().anyMatch(line -> Boolean.TRUE.equals(line.getManualFull())) ? 1 : 0);
        pallet.setActualWeightKg(lines.stream().map(InboundPutawayDTO.PutawayLine::getActualWeightKg)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(pallet.getActualWeightKg()));
        pallet.setUpdateBy(currentUserId());
        palletMapper.updateById(pallet);
        return pallet;
    }

    public boolean hasCapacityForTransfer(Long warehouseId, String locationCode, Long erpTenantId,
            String skuCode, String quality) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        for (PalletSlotVO slot : listSlots(warehouseId)) {
            if (!locationCode.equals(slot.getLocationCode())) {
                continue;
            }
            if (slot.getPalletId() == null) {
                return true;
            }
            if (PARTIAL.equals(slot.getPalletStatus())
                    && palletCanAccept(warehouse, slot.getPalletId(), erpTenantId, skuCode, quality)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEmptySlotForWholePallet(Long warehouseId, String locationCode) {
        WmsLocation location = physicalLocations(warehouseId).stream()
                .filter(item -> locationCode.equals(item.getLocationCode()))
                .findFirst().orElse(null);
        return location != null && slotMapper.selectCount(WrappersX.lambdaQueryX(WmsLocationSlot.class)
                .eq(WmsLocationSlot::getWarehouseId, warehouseId)
                .eq(WmsLocationSlot::getLocationId, location.getId())
                .eq(WmsLocationSlot::getSlotStatus, EMPTY)) > 0;
    }

    public boolean canPalletAcceptTransfer(Long warehouseId, Long palletId, Long erpTenantId,
            String skuCode, String quality) {
        WmsPallet pallet = palletMapper.selectById(palletId);
        if (pallet == null || !warehouseId.equals(pallet.getWarehouseId())
                || pallet.getCurrentSlotId() == null || !PARTIAL.equals(pallet.getPalletStatus())) {
            return false;
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null && palletCanAccept(warehouse, palletId, erpTenantId, skuCode, quality);
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsLocationSlot moveWholePallet(Long palletId, String targetLocationCode) {
        WmsPallet source = palletMapper.selectById(palletId);
        Assert.notNull(source, "源托盘不存在");
        WmsLocation target = physicalLocations(source.getWarehouseId()).stream()
                .filter(item -> targetLocationCode.equals(item.getLocationCode()))
                .findFirst().orElse(null);
        Assert.notNull(target, "目标物理库位不存在：" + targetLocationCode);
        WmsLocationSlot targetSlot = slotMapper.selectFirstEmptyForUpdate(
                target.getWarehouseId(), target.getId());
        Assert.notNull(targetSlot, "目标库位没有空托位：" + targetLocationCode);
        return moveWholePallet(palletId, targetLocationCode, targetSlot.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsLocationSlot moveWholePallet(Long palletId, String targetLocationCode, Long targetSlotId) {
        return moveWholePallet(palletId, targetLocationCode, targetSlotId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsLocationSlot moveWholePalletFromVirtual(Long palletId, String targetLocationCode) {
        WmsPallet source = palletMapper.selectById(palletId);
        Assert.notNull(source, "源托盘不存在");
        WmsLocation target = physicalLocations(source.getWarehouseId()).stream()
                .filter(item -> targetLocationCode.equals(item.getLocationCode()))
                .findFirst().orElse(null);
        Assert.notNull(target, "目标物理库位不存在：" + targetLocationCode);
        WmsLocationSlot targetSlot = slotMapper.selectFirstEmptyForUpdate(
                target.getWarehouseId(), target.getId());
        Assert.notNull(targetSlot, "目标库位没有空托位：" + targetLocationCode);
        return moveWholePallet(palletId, targetLocationCode, targetSlot.getId(), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsLocationSlot moveWholePalletFromVirtual(Long palletId, String targetLocationCode, Long targetSlotId) {
        return moveWholePallet(palletId, targetLocationCode, targetSlotId, false);
    }

    private WmsLocationSlot moveWholePallet(Long palletId, String targetLocationCode, Long targetSlotId,
            boolean releasePhysicalSource) {
        WmsPallet pallet = palletMapper.selectForUpdate(palletId);
        Assert.notNull(pallet, "源托盘不存在");
        Assert.isTrue(!CLOSED.equals(pallet.getPalletStatus()), "源托盘已关闭");
        WmsLocationSlot sourceSlot = !releasePhysicalSource || pallet.getCurrentSlotId() == null
                ? null : slotMapper.selectForUpdate(pallet.getCurrentSlotId());
        if (releasePhysicalSource && pallet.getCurrentSlotId() != null) {
            Assert.notNull(sourceSlot, "源托位不存在");
        }
        WmsLocation target = physicalLocations(pallet.getWarehouseId()).stream()
                .filter(item -> targetLocationCode.equals(item.getLocationCode()))
                .findFirst().orElse(null);
        Assert.notNull(target, "目标物理库位不存在：" + targetLocationCode);
        target = locationMapper.selectByIdForUpdate(target.getId());
        Assert.isTrue(!Integer.valueOf(1).equals(target.getIsVirtual()), "整托只能移动到物理库位");
        WmsLocationSlot targetSlot = slotMapper.selectForUpdate(targetSlotId);
        Assert.notNull(targetSlot, "目标托位不存在");
        Assert.isTrue(pallet.getWarehouseId().equals(targetSlot.getWarehouseId())
                && target.getId().equals(targetSlot.getLocationId()), "目标托位不属于所选目标库位");
        Assert.isTrue(sourceSlot == null || !sourceSlot.getId().equals(targetSlot.getId()),
                "目标托位不能与源托位相同");
        if (slotMapper.claim(targetSlot.getId()) != 1) {
            throw new BusinessException(409, "目标托位已被占用，请刷新后重试：" + targetSlot.getSlotCode());
        }
        String sourceSlotCode = pallet.getSlotCode();
        pallet.setSlotId(targetSlot.getId());
        pallet.setCurrentSlotId(targetSlot.getId());
        pallet.setSlotCode(targetSlot.getSlotCode());
        pallet.setLabelVersion(nextLabelVersion(pallet.getLabelVersion()));
        pallet.setUpdateBy(currentUserId());
        palletMapper.updateById(pallet);
        if (sourceSlot != null) {
            Assert.isTrue(slotMapper.release(sourceSlot.getId()) == 1, "源托位释放失败，请重试");
        }
        writeLog(pallet, "MOVE", sourceSlotCode, targetSlot.getSlotCode(), "整托库位调整");
        return targetSlot;
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveWholePalletToVirtual(Long palletId, String targetLocationCode) {
        WmsPallet pallet = palletMapper.selectForUpdate(palletId);
        Assert.notNull(pallet, "源托盘不存在");
        Assert.isTrue(!CLOSED.equals(pallet.getPalletStatus()), "源托盘已关闭");
        WmsLocation target = locationMapper.selectList(WrappersX.lambdaQueryX(WmsLocation.class)
                        .eq(WmsLocation::getWarehouseId, pallet.getWarehouseId())
                        .eq(WmsLocation::getLocationCode, targetLocationCode))
                .stream().findFirst().orElse(null);
        Assert.isTrue(target != null && Integer.valueOf(1).equals(target.getIsVirtual()),
                "目标虚拟库位不存在：" + targetLocationCode);
        WmsLocationSlot sourceSlot = pallet.getCurrentSlotId() == null
                ? null : slotMapper.selectForUpdate(pallet.getCurrentSlotId());
        if (pallet.getCurrentSlotId() != null) {
            Assert.notNull(sourceSlot, "源托位不存在");
        }
        String sourceSlotCode = pallet.getSlotCode();
        pallet.setCurrentSlotId(null);
        pallet.setSlotCode(targetLocationCode);
        pallet.setLabelVersion(nextLabelVersion(pallet.getLabelVersion()));
        pallet.setUpdateBy(currentUserId());
        palletMapper.updateById(pallet);
        if (sourceSlot != null) {
            Assert.isTrue(slotMapper.release(sourceSlot.getId()) == 1, "源托位释放失败，请重试");
        }
        writeLog(pallet, "MOVE_VIRTUAL", sourceSlotCode, targetLocationCode, "整托移入虚拟库位");
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsPallet allocateForTransfer(Long warehouseId, String locationCode, Long erpTenantId,
            String skuCode, String quality, int quantity) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        Assert.notNull(warehouse, "仓库不存在");
        InboundPutawayDTO.PutawayLine line = new InboundPutawayDTO.PutawayLine();
        line.setSkuCode(skuCode);
        line.setQuality(quality);
        line.setQuantity(quantity);
        line.setCapacitySource("MANUAL_REQUIRED");
        List<InboundPutawayDTO.PutawayLine> lines = Collections.singletonList(line);
        List<PalletSlotVO> slots = listSlots(warehouseId).stream()
                .filter(slot -> locationCode.equals(slot.getLocationCode()))
                .collect(Collectors.toList());

        for (PalletSlotVO slot : slots) {
            if (slot.getPalletId() != null && PARTIAL.equals(slot.getPalletStatus())
                    && palletCanAccept(warehouse, slot.getPalletId(), erpTenantId, skuCode, quality)) {
                return lockExistingForPutaway(slot.getPalletId(), erpTenantId, lines);
            }
        }
        for (PalletSlotVO slot : slots) {
            if (slot.getPalletId() == null) {
                line.setSlotCode(slot.getSlotCode());
                return createForPutaway(warehouseId, erpTenantId, slot.getSlotCode(), lines);
            }
        }
        throw new BusinessException(409, "目标库位没有可用托位或可合并托盘：" + locationCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsPallet allocateForTransfer(Long warehouseId, String locationCode, Long targetSlotId,
            Long targetPalletId, Long erpTenantId, String skuCode, String quality, int quantity) {
        Assert.isTrue((targetSlotId == null) != (targetPalletId == null),
                "拆零移库必须选择一个目标空托位或目标半托盘");
        InboundPutawayDTO.PutawayLine line = new InboundPutawayDTO.PutawayLine();
        line.setSkuCode(skuCode);
        line.setQuality(quality);
        line.setQuantity(quantity);
        line.setCapacitySource("MANUAL_REQUIRED");
        List<InboundPutawayDTO.PutawayLine> lines = Collections.singletonList(line);
        if (targetPalletId != null) {
            WmsPallet pallet = palletMapper.selectForUpdate(targetPalletId);
            Assert.notNull(pallet, "目标托盘不存在");
            Assert.isTrue(warehouseId.equals(pallet.getWarehouseId())
                    && pallet.getCurrentSlotId() != null, "目标托盘不在所选仓库");
            WmsLocationSlot slot = slotMapper.selectForUpdate(pallet.getCurrentSlotId());
            WmsLocation location = slot == null ? null : locationMapper.selectByIdForUpdate(slot.getLocationId());
            Assert.isTrue(location != null && locationCode.equals(location.getLocationCode()),
                    "目标托盘不属于所选目标库位");
            return lockExistingForPutaway(targetPalletId, erpTenantId, lines);
        }
        WmsLocationSlot slot = slotMapper.selectForUpdate(targetSlotId);
        Assert.notNull(slot, "目标托位不存在");
        WmsLocation location = locationMapper.selectByIdForUpdate(slot.getLocationId());
        Assert.isTrue(location != null && warehouseId.equals(location.getWarehouseId())
                && locationCode.equals(location.getLocationCode())
                && !Integer.valueOf(1).equals(location.getIsVirtual()), "目标托位不属于所选目标库位");
        return createForPutaway(warehouseId, erpTenantId, slot.getSlotCode(), lines);
    }

    private boolean palletCanAccept(Warehouse warehouse, Long palletId, Long erpTenantId,
            String skuCode, String quality) {
        List<WmsPhysicalInventory> current = physicalInventoryMapper.selectList(
                WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
                        .eq(WmsPhysicalInventory::getPalletId, palletId)
                        .gt(WmsPhysicalInventory::getQuantity, 0));
        if (current.stream().anyMatch(item -> !erpTenantId.equals(item.getErpTenantId())
                || !normalizeQuality(quality).equals(normalizeQuality(item.getQuality())))) {
            return false;
        }
        int maxKinds = warehouse.getMaxSkuKindsPerPallet() == null ? 4 : warehouse.getMaxSkuKindsPerPallet();
        Set<String> kinds = current.stream().map(item -> item.getErpTenantId() + "|" + item.getSkuCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        kinds.add(erpTenantId + "|" + skuCode);
        return kinds.size() <= maxKinds;
    }

    private void validateCapacityAndMixing(Warehouse warehouse, Long erpTenantId,
            List<WmsPhysicalInventory> current, List<InboundPutawayDTO.PutawayLine> incoming,
            WmsLocationSlot slot) {
        int maxKinds = warehouse.getMaxSkuKindsPerPallet() == null ? 4 : warehouse.getMaxSkuKindsPerPallet();
        Set<String> owners = current.stream().map(item -> String.valueOf(item.getErpTenantId()))
                .collect(Collectors.toSet());
        Set<String> kinds = current.stream().map(item -> item.getErpTenantId() + "|" + item.getSkuCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        owners.add(String.valueOf(erpTenantId));
        incoming.forEach(line -> kinds.add(erpTenantId + "|" + line.getSkuCode()));
        Assert.isTrue(kinds.size() <= maxKinds, "单托最多允许 " + maxKinds + " 种不同货物");
        Assert.isTrue(owners.size() <= 1, "同一托盘禁止跨货主混托");
        BigDecimal percent = incoming.stream().map(InboundPutawayDTO.PutawayLine::getCapacityPercent)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(null);
        Assert.isTrue(percent == null || percent.compareTo(new BigDecimal("100")) <= 0,
                "托盘容量不能超过100%");
        BigDecimal weight = incoming.stream().map(InboundPutawayDTO.PutawayLine::getActualWeightKg)
                .filter(value -> value != null).max(BigDecimal::compareTo).orElse(null);
        BigDecimal maxWeight = slot.getMaxWeightKg() == null
                ? warehouse.getDefaultPalletMaxWeightKg() : slot.getMaxWeightKg();
        Assert.isTrue(weight == null || maxWeight == null || weight.compareTo(maxWeight) <= 0,
                "托盘重量超过层位安全承重 " + maxWeight + " kg");
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAfterInventoryChange(Long palletId) {
        if (palletId == null) {
            return;
        }
        WmsPallet pallet = palletMapper.selectForUpdate(palletId);
        if (pallet == null) {
            return;
        }
        List<WmsPhysicalInventory> batches = physicalInventoryMapper.selectList(
                WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
                        .eq(WmsPhysicalInventory::getPalletId, palletId)
                        .gt(WmsPhysicalInventory::getQuantity, 0));
        if (batches.isEmpty()) {
            Long slotId = pallet.getCurrentSlotId();
            pallet.setCurrentSlotId(null);
            pallet.setPalletStatus(CLOSED);
            pallet.setWholePalletEligible(0);
            pallet.setManualFull(0);
            pallet.setSkuKindCount(0);
            pallet.setLabelVersion(nextLabelVersion(pallet.getLabelVersion()));
            pallet.setUpdateBy(currentUserId());
            palletMapper.updateById(pallet);
            if (slotId != null) {
                slotMapper.release(slotId);
            }
            writeLog(pallet, "CLOSE", pallet.getSlotCode(), null, "Pallet emptied or shipped");
            return;
        }
        int kinds = (int) batches.stream().map(item -> item.getErpTenantId() + "|" + item.getSkuCode()).distinct().count();
        boolean full = Integer.valueOf(1).equals(pallet.getManualFull())
                || (pallet.getCapacityPercent() != null
                && pallet.getCapacityPercent().compareTo(new BigDecimal("100")) >= 0);
        if ("FULL".equals(pallet.getPalletStatus()) && batches.stream().anyMatch(batch ->
                value(batch.getReservedQty()) > 0 && value(batch.getReservedQty()) < value(batch.getQuantity()))) {
            full = false;
            pallet.setCapacityPercent(null);
            pallet.setCapacitySource("MANUAL_REQUIRED");
            pallet.setManualFull(0);
        }
        pallet.setSkuKindCount(kinds);
        pallet.setPalletType(kinds > 1 ? "MIXED" : full ? "SINGLE_FULL" : "SINGLE_PARTIAL");
        if (!"LOCKED".equals(pallet.getPalletStatus()) && !"ALLOCATED".equals(pallet.getPalletStatus())
                && !"PICKING".equals(pallet.getPalletStatus())) {
            pallet.setPalletStatus(full ? FULL : PARTIAL);
        }
        pallet.setWholePalletEligible(full && kinds == 1 ? 1 : 0);
        pallet.setLabelVersion(nextLabelVersion(pallet.getLabelVersion()));
        pallet.setUpdateBy(currentUserId());
        palletMapper.updateById(pallet);
    }

    private int nextLabelVersion(Integer current) {
        return current == null ? 1 : current + 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAfterOutbound(Long palletId) {
        refreshAfterInventoryChange(palletId);
        WmsPallet pallet = palletMapper.selectForUpdate(palletId);
        if (pallet == null || CLOSED.equals(pallet.getPalletStatus())) {
            return;
        }
        // A full pallet that still contains stock after shipment was broken for loose picking.
        if (FULL.equals(pallet.getPalletStatus())) {
            pallet.setPalletStatus(PARTIAL);
            pallet.setPalletType(value(pallet.getSkuKindCount()) > 1 ? "MIXED" : "SINGLE_PARTIAL");
            pallet.setWholePalletEligible(0);
            pallet.setCapacityPercent(null);
            pallet.setCapacitySource("MANUAL_REQUIRED");
            pallet.setManualFull(0);
            pallet.setUpdateBy(currentUserId());
            palletMapper.updateById(pallet);
            writeLog(pallet, "BREAK_FULL", pallet.getSlotCode(), pallet.getSlotCode(), "Partial outbound from full pallet");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public WmsPallet calibrate(PalletCapacityDTO dto) {
        WmsPallet pallet = palletMapper.selectForUpdate(dto.getPalletId());
        Assert.notNull(pallet, "托盘不存在");
        Assert.isTrue(pallet.getCurrentSlotId() != null, "已关闭托盘不能校准");
        WmsLocationSlot slot = slotMapper.selectForUpdate(pallet.getCurrentSlotId());
        Warehouse warehouse = warehouseMapper.selectById(pallet.getWarehouseId());
        BigDecimal maxWeight = slot.getMaxWeightKg() == null
                ? warehouse.getDefaultPalletMaxWeightKg() : slot.getMaxWeightKg();
        Assert.isTrue(dto.getActualWeightKg() == null || maxWeight == null
                || dto.getActualWeightKg().compareTo(maxWeight) <= 0,
                "实际重量超过层位安全承重 " + maxWeight + " kg");
        boolean full = Boolean.TRUE.equals(dto.getMarkFull())
                || dto.getCapacityPercent().compareTo(new BigDecimal("100")) >= 0;
        pallet.setCapacityPercent(dto.getCapacityPercent());
        pallet.setCapacitySource("MANUAL");
        pallet.setActualWeightKg(dto.getActualWeightKg());
        pallet.setPalletStatus(full ? FULL : PARTIAL);
        pallet.setPalletType(value(pallet.getSkuKindCount()) > 1 ? "MIXED" : full ? "SINGLE_FULL" : "SINGLE_PARTIAL");
        pallet.setWholePalletEligible(full && value(pallet.getSkuKindCount()) == 1 ? 1 : 0);
        pallet.setManualFull(Boolean.TRUE.equals(dto.getMarkFull()) ? 1 : 0);
        pallet.setUpdateBy(currentUserId());
        palletMapper.updateById(pallet);
        writeLog(pallet, "CALIBRATE", pallet.getSlotCode(), pallet.getSlotCode(), dto.getRemark());
        return pallet;
    }

    public List<PalletSummaryVO> listPallets(Long warehouseId, Long erpTenantId, String skuCode, String status) {
        List<PalletSummaryVO> result = listPallets(warehouseId, erpTenantId, null, null, null, status);
        if (skuCode == null || skuCode.isEmpty()) {
            return result;
        }
        return result.stream()
                .filter(vo -> vo.getItems().stream()
                        .anyMatch(item -> item.getSkuCode() != null && item.getSkuCode().contains(skuCode)))
                .collect(Collectors.toList());
    }

    public List<PalletSummaryVO> listPallets(Long warehouseId, Long erpTenantId, Long wmsTenantId,
            String palletNo, String slotCode, String status) {
        List<WmsPallet> pallets = palletMapper.selectList(WrappersX.lambdaQueryX(WmsPallet.class)
                .eqIfPresent(WmsPallet::getWarehouseId, warehouseId)
                .eqIfPresent(WmsPallet::getErpTenantId, erpTenantId)
                .eqIfPresent(WmsPallet::getWmsTenantId, wmsTenantId)
                .likeIfPresent(WmsPallet::getPalletNo, palletNo)
                .likeIfPresent(WmsPallet::getSlotCode, slotCode)
                .eqIfPresent(WmsPallet::getPalletStatus, status)
                .orderByDesc(WmsPallet::getCreateTime));
        List<PalletSummaryVO> result = pallets.stream()
                .map(pallet -> toSummary(pallet, true))
                .collect(Collectors.toList());
        result.sort(Comparator
                .comparing(PalletSummaryVO::getWarehouseName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(PalletSummaryVO::getWarehouseId,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparing(PalletSummaryVO::getSlotCode, WmsPalletService::compareNatural)
                .thenComparing(PalletSummaryVO::getPalletNo, WmsPalletService::compareNatural));
        return result;
    }

    public static int compareNatural(String left, String right) {
        if (left == null || right == null) {
            return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER).compare(left, right);
        }
        int li = 0;
        int ri = 0;
        while (li < left.length() && ri < right.length()) {
            char lc = left.charAt(li);
            char rc = right.charAt(ri);
            if (Character.isDigit(lc) && Character.isDigit(rc)) {
                int le = li;
                int re = ri;
                while (le < left.length() && Character.isDigit(left.charAt(le))) {
                    le++;
                }
                while (re < right.length() && Character.isDigit(right.charAt(re))) {
                    re++;
                }
                String leftNumber = left.substring(li, le);
                String rightNumber = right.substring(ri, re);
                int numberCompare = new BigInteger(leftNumber).compareTo(new BigInteger(rightNumber));
                if (numberCompare != 0) {
                    return numberCompare;
                }
                int widthCompare = Integer.compare(leftNumber.length(), rightNumber.length());
                if (widthCompare != 0) {
                    return widthCompare;
                }
                li = le;
                ri = re;
                continue;
            }
            int charCompare = Character.compare(Character.toLowerCase(lc), Character.toLowerCase(rc));
            if (charCompare != 0) {
                return charCompare;
            }
            li++;
            ri++;
        }
        return Integer.compare(left.length(), right.length());
    }

    public PalletSummaryVO getDetail(Long id) {
        WmsPallet pallet = palletMapper.selectById(id);
        Assert.notNull(pallet, "托盘不存在");
        return toSummary(pallet, true);
    }

    public List<PalletSummaryVO> summaries(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return palletMapper.selectBatchIds(ids).stream().map(pallet -> toSummary(pallet, true))
                .collect(Collectors.toList());
    }

    public List<PalletSummaryVO> listByInboundOrder(Long inboundOrderId) {
        return palletMapper.selectByInboundOrderId(inboundOrderId).stream()
                .map(pallet -> toSummary(pallet, true))
                .collect(Collectors.toList());
    }

    public List<PutawayReceiptLineVO> listReceiptLinesByInboundOrder(Long inboundOrderId) {
        List<PutawayReceiptLineVO> lines = palletMapper.selectReceiptLinesByInboundOrderId(inboundOrderId);
        lines.forEach(line -> line.setWarehouseSkuCode(
                warehouseSkuCodeService.build(line.getErpTenantId(), line.getSkuCode())));
        return lines;
    }

    private PalletSummaryVO toSummary(WmsPallet pallet, boolean withItems) {
        PalletSummaryVO vo = new PalletSummaryVO();
        vo.setId(pallet.getId());
        vo.setPalletNo(pallet.getPalletNo());
        vo.setWarehouseId(pallet.getWarehouseId());
        Warehouse warehouse = warehouseMapper.selectById(pallet.getWarehouseId());
        vo.setWarehouseName(warehouse == null ? null : warehouse.getWarehouseName());
        vo.setWmsTenantId(pallet.getWmsTenantId());
        vo.setErpTenantId(pallet.getErpTenantId());
        SysTenant operator = pallet.getWmsTenantId() == null ? null : tenantMapper.selectById(pallet.getWmsTenantId());
        SysTenant ownerTenant = pallet.getErpTenantId() == null ? null : tenantMapper.selectById(pallet.getErpTenantId());
        vo.setWmsTenantName(operator == null ? null : operator.getTenantName());
        vo.setOwnerName(ownerTenant == null ? null : ownerTenant.getTenantName());
        vo.setSlotCode(pallet.getSlotCode());
        vo.setPalletType(pallet.getPalletType());
        vo.setPalletStatus(pallet.getPalletStatus());
        vo.setCapacityPercent(pallet.getCapacityPercent());
        vo.setCapacitySource(pallet.getCapacitySource());
        vo.setEstimatedWeightKg(pallet.getEstimatedWeightKg());
        vo.setActualWeightKg(pallet.getActualWeightKg());
        vo.setSkuKindCount(pallet.getSkuKindCount());
        vo.setWholePalletEligible(pallet.getWholePalletEligible());
        vo.setLabelVersion(pallet.getLabelVersion());
        vo.setCreateTime(pallet.getCreateTime());
        if (withItems) {
            List<WmsPhysicalInventory> batches = physicalInventoryMapper.selectList(
                    WrappersX.lambdaQueryX(WmsPhysicalInventory.class)
                            .eq(WmsPhysicalInventory::getPalletId, pallet.getId())
                            .gt(WmsPhysicalInventory::getQuantity, 0));
            Map<Long, SysTenant> owners = new HashMap<>();
            Set<Long> ownerIds = batches.stream().map(WmsPhysicalInventory::getErpTenantId)
                    .filter(id -> id != null).collect(Collectors.toSet());
            if (!ownerIds.isEmpty()) {
                tenantMapper.selectBatchIds(ownerIds).forEach(owner -> owners.put(owner.getId(), owner));
            }
            Map<String, PalletSummaryVO.PalletItemVO> grouped = new LinkedHashMap<>();
            for (WmsPhysicalInventory batch : batches) {
                String key = batch.getErpTenantId() + "|" + batch.getSkuCode() + "|" + batch.getQuality();
                PalletSummaryVO.PalletItemVO item = grouped.computeIfAbsent(key, ignored -> {
                    PalletSummaryVO.PalletItemVO created = new PalletSummaryVO.PalletItemVO();
                    created.setErpTenantId(batch.getErpTenantId());
                    SysTenant owner = owners.get(batch.getErpTenantId());
                    created.setOwnerName(owner == null ? null : owner.getTenantName());
                    created.setSkuCode(batch.getSkuCode());
                    created.setWarehouseSkuCode(
                            warehouseSkuCodeService.build(batch.getErpTenantId(), batch.getSkuCode()));
                    created.setQuality(batch.getQuality());
                    created.setInboundDate(batch.getInboundDate() == null ? null : batch.getInboundDate().toString());
                    created.setQuantity(0);
                    created.setReservedQty(0);
                    return created;
                });
                item.setQuantity(value(item.getQuantity()) + value(batch.getQuantity()));
                item.setReservedQty(value(item.getReservedQty()) + value(batch.getReservedQty()));
            }
            vo.setItems(new ArrayList<>(grouped.values()));
        }
        return vo;
    }

    private void writeLog(WmsPallet pallet, String operation, String from, String to, String remark) {
        WmsPalletOperationLog log = new WmsPalletOperationLog();
        log.setPalletId(pallet.getId());
        log.setOperationType(operation);
        log.setFromSlotCode(from);
        log.setToSlotCode(to);
        log.setCapacityPercent(pallet.getCapacityPercent());
        log.setRemark(remark);
        log.setOperatorId(currentUserId());
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String nextPalletNo() {
        return "PLT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private Long currentUserId() {
        try {
            return principalAttributeAccessor.getUserId();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String normalizeQuality(String quality) {
        return "DAMAGED".equalsIgnoreCase(quality) ? "DAMAGED" : "GOOD";
    }
}
