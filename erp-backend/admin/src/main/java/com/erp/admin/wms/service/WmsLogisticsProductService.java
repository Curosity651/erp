package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.dto.LogisticsProductDTO;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import com.erp.admin.wms.model.vo.LogisticsProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.core.exception.BusinessException;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.util.JsonUtils;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WMS 服务商物流产品服务。管理端仅服务商本人（wms_tenant_id=自己）；
 * 货主端只读其父服务商启用中的产品（建单选项）。
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsLogisticsProductService extends ExtendServiceImpl<WmsLogisticsProductMapper, WmsLogisticsProduct> {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TenantIdentityService tenantIdentityService;

    private final SysTenantMapper sysTenantMapper;

    // ==================== 服务商管理端 ====================

    public PageResult<LogisticsProductVO> page(PageParam pageParam, String keyword, Integer status) {
        Long wmsTenantId = currentOperatorId();
        IPage<WmsLogisticsProduct> page = baseMapper.pageByTenant(pageParam, wmsTenantId, keyword, status);
        List<LogisticsProductVO> records = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(LogisticsProductDTO dto) {
        Long wmsTenantId = currentOperatorId();
        Assert.isTrue(dto.getUnitPrice() != null && dto.getUnitPrice().compareTo(BigDecimal.ZERO) >= 0, "单价不能为负");

        WmsLogisticsProduct entity;
        if (dto.getId() != null) {
            entity = loadOwned(dto.getId(), wmsTenantId);
        } else {
            entity = new WmsLogisticsProduct();
            entity.setWmsTenantId(wmsTenantId);
            entity.setStatus(1);
        }
        entity.setProductName(dto.getProductName());
        entity.setProductCode(dto.getProductCode());
        entity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null : JsonUtils.toJson(dto.getTags()));
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setRemark(dto.getRemark());
        this.saveOrUpdate(entity);
        log.info("物流产品保存, id={}, wmsTenantId={}, name={}", entity.getId(), wmsTenantId, dto.getProductName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Assert.isTrue(status != null && (status == 0 || status == 1), "状态非法");
        WmsLogisticsProduct entity = loadOwned(id, currentOperatorId());
        entity.setStatus(status);
        this.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        WmsLogisticsProduct entity = loadOwned(id, currentOperatorId());
        this.removeById(entity.getId());
    }

    // ==================== 货主端（建单选项） ====================

    /**
     * 货主可选物流产品：其父服务商启用中的产品。非货主身份或无父服务商 → 空列表。
     */
    public List<LogisticsProductVO> listOptionsForOwner() {
        Long erp = TenantContext.getCurrentTenant();
        if (erp == null || erp <= 0) {
            return Collections.emptyList();
        }
        SysTenant tenant = sysTenantMapper.selectById(erp);
        if (tenant == null || tenant.getParentWmsTenantId() == null) {
            return Collections.emptyList();
        }
        return baseMapper.listEnabledByTenant(tenant.getParentWmsTenantId())
            .stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }

    // ==================== 计费取价（签出用） ====================

    /**
     * 按产品 ID 取启用产品（签出计费取单价）。停用/不存在返回 null（计费按 0 处理，不阻断发货）。
     */
    public WmsLogisticsProduct getEnabledById(Long id) {
        if (id == null) {
            return null;
        }
        WmsLogisticsProduct product = this.getById(id);
        return (product != null && product.getStatus() != null && product.getStatus() == 1) ? product : null;
    }

    // ==================== 辅助 ====================

    private WmsLogisticsProduct loadOwned(Long id, Long wmsTenantId) {
        WmsLogisticsProduct entity = this.getById(id);
        Assert.notNull(entity, "物流产品不存在");
        if (!wmsTenantId.equals(entity.getWmsTenantId())) {
            throw new BusinessException(403, "无权操作其它服务商的物流产品");
        }
        return entity;
    }

    private Long currentOperatorId() {
        String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
        if (!TenantIdentityService.IDENTITY_WMS_OPERATOR.equals(identityType)) {
            throw new BusinessException(403, "仅WMS服务商可管理物流产品");
        }
        Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
        Assert.notNull(wmsTenantId, "服务商上下文缺失");
        return wmsTenantId;
    }

    private LogisticsProductVO toVo(WmsLogisticsProduct entity) {
        LogisticsProductVO vo = new LogisticsProductVO();
        vo.setId(entity.getId());
        vo.setProductName(entity.getProductName());
        vo.setProductCode(entity.getProductCode());
        vo.setTags(parseTags(entity.getTags()));
        vo.setUnitPrice(entity.getUnitPrice());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime() == null ? null : entity.getCreateTime().format(DT));
        return vo;
    }

    /** tags JSON → 词条列表（供本服务和收入页复用） */
    @SuppressWarnings("unchecked")
    public static List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return (List<String>) JsonUtils.toObj(tagsJson, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}
