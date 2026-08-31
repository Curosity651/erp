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
import java.util.Locale;
import java.util.Objects;
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
        List<LogisticsProductVO> records = page.getRecords().stream().map(this::toManagementVo)
            .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void createProduct(LogisticsProductDTO dto) {
        Long wmsTenantId = currentOperatorId();
        WmsLogisticsProduct entity = new WmsLogisticsProduct();
        entity.setWmsTenantId(wmsTenantId);
        entity.setStatus(1);
        fillProduct(entity, dto, wmsTenantId, null);
        this.save(entity);
        log.info("物流产品新建, id={}, wmsTenantId={}, name={}", entity.getId(), wmsTenantId, dto.getProductName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, LogisticsProductDTO dto) {
        Long wmsTenantId = currentOperatorId();
        WmsLogisticsProduct entity = loadOwned(id, wmsTenantId);
        assertPricingIdentityEditable(entity, dto, wmsTenantId);
        fillProduct(entity, dto, wmsTenantId, id);
        this.updateById(entity);
        log.info("物流产品编辑, id={}, wmsTenantId={}, name={}", entity.getId(), wmsTenantId, dto.getProductName());
    }

    private void fillProduct(WmsLogisticsProduct entity, LogisticsProductDTO dto, Long wmsTenantId, Long excludeId) {
        Assert.isTrue(dto.getUnitPrice() != null && dto.getUnitPrice().compareTo(BigDecimal.ZERO) >= 0, "单价不能为负");
        Assert.hasText(dto.getProductCode(), "产品编码不能为空");
        Assert.hasText(dto.getCurrency(), "币种不能为空");
        String productCode = dto.getProductCode().trim().toUpperCase(Locale.ROOT);
        Long duplicateCount = baseMapper.countByCode(wmsTenantId, productCode, excludeId);
        Assert.isTrue(duplicateCount == null || duplicateCount == 0, "产品编码已存在");
        entity.setProductName(dto.getProductName());
        entity.setProductCode(productCode);
        entity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null : JsonUtils.toJson(dto.getTags()));
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setCurrency(dto.getCurrency().trim().toUpperCase(Locale.ROOT));
        entity.setProductDescription(dto.getProductDescription());
        entity.setRemark(dto.getRemark());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Assert.isTrue(status != null && (status == 0 || status == 1), "状态非法");
        Long wmsTenantId = currentOperatorId();
        WmsLogisticsProduct entity = loadOwned(id, wmsTenantId);
        if (status == 0 && !Integer.valueOf(0).equals(entity.getStatus())) {
            long shopCount = nz(baseMapper.countShopReferences(wmsTenantId, id));
            Assert.isTrue(shopCount == 0, "该物流产品仍是 " + shopCount
                + " 个店铺的默认物流产品，请先修改店铺默认物流产品后再停用");
        }
        entity.setStatus(status);
        this.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Long wmsTenantId = currentOperatorId();
        loadOwned(id, wmsTenantId);
        throw new BusinessException(400, "物流产品不能删除，请使用停用功能保留历史业务记录");
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

	public WmsLogisticsProduct requireEnabledForOwner(Long productId, Long erpTenantId) {
		Assert.notNull(productId, "请选择物流产品");
		Assert.notNull(erpTenantId, "货主不能为空");
		SysTenant owner = sysTenantMapper.selectById(erpTenantId);
		Assert.notNull(owner, "货主不存在");
		Assert.isTrue(TenantIdentityService.IDENTITY_ERP_USER.equals(owner.getTenantType()), "当前对象不是ERP货主");
		Assert.notNull(owner.getParentWmsTenantId(), "货主未绑定WMS服务商");
		WmsLogisticsProduct product = this.getById(productId);
		Assert.isTrue(product != null && Integer.valueOf(1).equals(product.getStatus()), "物流产品不可用");
		Assert.isTrue(owner.getParentWmsTenantId().equals(product.getWmsTenantId()), "物流产品不属于当前服务商");
		return product;
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

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    private void assertPricingIdentityEditable(WmsLogisticsProduct entity, LogisticsProductDTO dto,
            Long wmsTenantId) {
        if (orderReferenceCount(wmsTenantId, entity.getId()) == 0) {
            return;
        }
        String requestedCode = dto.getProductCode() == null ? null
            : dto.getProductCode().trim().toUpperCase(Locale.ROOT);
        String requestedCurrency = dto.getCurrency() == null ? null
            : dto.getCurrency().trim().toUpperCase(Locale.ROOT);
        boolean codeChanged = !Objects.equals(entity.getProductCode(), requestedCode);
        boolean currencyChanged = !Objects.equals(entity.getCurrency(), requestedCurrency);
        boolean priceChanged = entity.getUnitPrice() == null || dto.getUnitPrice() == null
            || entity.getUnitPrice().compareTo(dto.getUnitPrice()) != 0;
        Assert.isTrue(!codeChanged && !currencyChanged && !priceChanged,
            "该物流产品已产生业务数据，产品编码、默认费用和币种不能修改；如需调价请使用复制调价");
    }

    private long orderReferenceCount(Long wmsTenantId, Long productId) {
        return nz(baseMapper.countFulfillmentReferences(wmsTenantId, productId))
            + nz(baseMapper.countLegacyOutboundReferences(wmsTenantId, productId));
    }

    private LogisticsProductVO toManagementVo(WmsLogisticsProduct entity) {
        LogisticsProductVO vo = toVo(entity);
        Long wmsTenantId = entity.getWmsTenantId();
        long shopCount = nz(baseMapper.countShopReferences(wmsTenantId, entity.getId()));
        long orderCount = orderReferenceCount(wmsTenantId, entity.getId());
        vo.setShopReferenceCount(shopCount);
        vo.setOrderReferenceCount(orderCount);
        vo.setUsed(orderCount > 0);
        return vo;
    }

    private LogisticsProductVO toVo(WmsLogisticsProduct entity) {
        LogisticsProductVO vo = new LogisticsProductVO();
        vo.setId(entity.getId());
        vo.setProductName(entity.getProductName());
        vo.setProductCode(entity.getProductCode());
        vo.setTags(parseTags(entity.getTags()));
        vo.setUnitPrice(entity.getUnitPrice());
		vo.setCurrency(entity.getCurrency());
		vo.setProductDescription(entity.getProductDescription());
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
