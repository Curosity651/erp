package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.FboInventorySnapshotMapper;
import com.erp.admin.wms.model.entity.FboInventorySnapshot;
import com.erp.admin.wms.model.dto.SkuQuantityDTO;
import com.erp.admin.wms.model.qo.FboInventoryQO;
import com.erp.admin.wms.model.vo.FboInventoryPageVO;
import com.erp.admin.wms.model.vo.FboInventorySummaryVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FboInventorySnapshotService
        extends ExtendServiceImpl<FboInventorySnapshotMapper, FboInventorySnapshot> {

    private static final int STALE_HOURS = 24;

    private final SkuBriefService skuBriefService;

    public PageResult<FboInventoryPageVO> queryPage(PageParam pageParam, FboInventoryQO qo) {
        Long tenantId = requireOwnerTenant();
        IPage<FboInventoryPageVO> page = baseMapper.queryPage(PageUtil.prodPage(pageParam), tenantId, qo);
        LocalDateTime staleBefore = LocalDateTime.now().minusHours(STALE_HOURS);
        page.getRecords().forEach(row -> row.setStale(row.getSyncedAt() == null || row.getSyncedAt().isBefore(staleBefore)));
        skuBriefService.enrichForQuery(page.getRecords(), FboInventoryPageVO::getSkuCode,
                FboInventoryPageVO::setSkuBrief);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public FboInventorySummaryVO getSummary() {
        FboInventorySummaryVO summary = baseMapper.selectSummary(requireOwnerTenant());
        if (summary == null) {
            summary = new FboInventorySummaryVO();
            summary.setSkuCount(0);
            summary.setShopCount(0);
            summary.setTotalQuantity(0);
        }
        summary.setStale(summary.getLastSyncedAt() == null
                || summary.getLastSyncedAt().isBefore(LocalDateTime.now().minusHours(STALE_HOURS)));
        return summary;
    }

    public Map<String, Integer> sumQuantityBySku(Long tenantId, Collection<String> skuCodes) {
        if (tenantId == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> result = new HashMap<>();
        for (SkuQuantityDTO row : baseMapper.sumQuantityBySku(tenantId, skuCodes)) {
            if (row.getSkuCode() != null && row.getQuantity() != null) {
                result.put(row.getSkuCode(), row.getQuantity());
            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceShopSnapshot(Long tenantId, String platform, Long shopId,
            List<FboInventorySnapshot> newRows) {
        this.baseMapper.delete(WrappersX.lambdaQueryX(FboInventorySnapshot.class)
                .eq(FboInventorySnapshot::getTenantId, tenantId)
                .eq(FboInventorySnapshot::getPlatform, platform)
                .eq(FboInventorySnapshot::getShopId, shopId));
        if (newRows != null && !newRows.isEmpty()) {
            this.saveBatch(newRows);
        }
    }

    private Long requireOwnerTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || TenantContext.BLOCK_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException("FBO inventory is available only to ERP owners");
        }
        return tenantId;
    }
}
