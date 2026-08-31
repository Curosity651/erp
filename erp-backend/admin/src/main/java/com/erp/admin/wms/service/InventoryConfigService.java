package com.erp.admin.wms.service;

import cn.hutool.core.util.StrUtil;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.wms.mapper.InventoryConfigMapper;
import com.erp.admin.wms.model.dto.GlobalInventoryConfigDTO;
import com.erp.admin.wms.model.dto.InventoryConfigDTO;
import com.erp.admin.wms.model.dto.EffectiveInventoryConfig;
import com.erp.admin.wms.model.entity.InventoryAlertConfig;
import com.erp.admin.wms.model.entity.InventoryConfig;
import com.erp.admin.wms.model.vo.GlobalInventoryConfigVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.business.infra.model.entity.SysConfig;
import org.ballcat.business.infra.service.SysConfigService;
import org.ballcat.business.system.model.entity.SysUser;
import org.ballcat.business.system.service.SysUserService;
import org.ballcat.common.util.JsonUtils;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.util.Assert;

/**
 * 库存配置服务
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class InventoryConfigService extends ExtendServiceImpl<InventoryConfigMapper, InventoryConfig> {

    private static final String GLOBAL_CONFIG_KEY = "wms.inventory.alert";

    /**
     * 全局(预警)配置按货主隔离：以 {@code wms.inventory.alert:{erpTenantId}} 为键，各货主独立一份。
     * 平台/无上下文用哨兵后缀，互不影响货主。修复前为单一共享键导致所有租户共用。
     */
    private String globalConfigKey() {
        Long erp = TenantContext.getCurrentTenant();
        return GLOBAL_CONFIG_KEY + ":" + (erp == null ? 0L : erp);
    }

    /**
     * 当前货主（SKU 配置三键查询用）。无上下文时为 null → mapper 按 -1 兜底命中 0 行，
     * 上层回退全局默认，fail-closed，绝不串到别的货主的配置。
     */
    private Long currentErp() {
        return TenantContext.getCurrentTenant();
    }

    private final SysConfigService sysConfigService;
    private final SysUserService sysUserService;
	private final PrincipalAttributeAccessor principalAttributeAccessor;


	// ========== 全局配置 ==========

    /**
     * 获取全局配置
     */
    public GlobalInventoryConfigVO getGlobalConfig() {
		InventoryAlertConfig config = this.getGlobalAlertConfig();

		GlobalInventoryConfigVO vo = new GlobalInventoryConfigVO();
		vo.setSafetyStock(config.getSafetyStock());
        vo.setNotifyEnabled(config.getNotifyEnabled());
        vo.setNotifyThresholdDays(config.getNotifyThresholdDays());
        vo.setNotifyUserIds(config.getNotifyUserIds() != null ? config.getNotifyUserIds() : Collections.emptyList());

        // 查询用户名称
        if (vo.getNotifyUserIds() != null && !vo.getNotifyUserIds().isEmpty()) {
            List<SysUser> users = sysUserService.listByUserIds(vo.getNotifyUserIds());
            vo.setNotifyUserNames(users.stream()
                    .map(SysUser::getNickname)
                    .collect(Collectors.toList()));
        } else {
            vo.setNotifyUserNames(Collections.emptyList());
        }

        return vo;
    }

    /**
     * 获取全局配置（内部使用，返回配置实体）
     */
    public InventoryAlertConfig getGlobalAlertConfig() {
		String value = sysConfigService.getConfValueByKey(globalConfigKey());
        if (StrUtil.isNotBlank(value)) {
            return JsonUtils.toObj(value, InventoryAlertConfig.class);
        }
        return new InventoryAlertConfig();
    }

    /**
     * 保存全局配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveGlobalConfig(GlobalInventoryConfigDTO dto) {
        InventoryAlertConfig config = new InventoryAlertConfig();
        config.setSafetyStock(dto.getSafetyStock());
        config.setNotifyEnabled(dto.getNotifyEnabled());
        config.setNotifyThresholdDays(dto.getNotifyThresholdDays());
        config.setNotifyUserIds(dto.getNotifyUserIds());

        String confValue = JsonUtils.toJson(config);

        // 适配:原作者私有的 getByKey 不存在,改用 ballcat 公开 API(getConfValueByKey 判存在)
        String scopedKey = globalConfigKey();
        String existingValue = sysConfigService.getConfValueByKey(scopedKey);
        SysConfig sysConfig = new SysConfig();
        sysConfig.setConfKey(scopedKey);
        sysConfig.setConfValue(confValue);
        if (existingValue == null) {
            sysConfig.setCategory("wms");
            sysConfig.setName("库存预警配置");
            sysConfigService.save(sysConfig);
        } else {
            sysConfigService.updateByKey(sysConfig);
        }
    }

    // ========== SKU 独立配置 ==========

    /**
     * 保存 SKU 配置（区域维度）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuConfig(InventoryConfigDTO dto) {
        // 校验：safetyStock 和 notifyThresholdDays 不能同时为空
        if (dto.getSafetyStock() == null && dto.getNotifyThresholdDays() == null) {
            throw new IllegalArgumentException("安全库存和预警阈值不能同时为空，请删除此配置");
        }

        // 校验：区域ID必填
        if (dto.getRegionId() == null) {
            throw new IllegalArgumentException("区域ID不能为空");
        }

        InventoryConfig entity;

        if (dto.getId() != null) {
            entity = baseMapper.selectOne(WrappersX.lambdaQueryX(InventoryConfig.class)
                    .eq(InventoryConfig::getId, dto.getId())
                    .eq(InventoryConfig::getErpTenantId, currentErp() == null ? -1L : currentErp()));
            if (entity == null) {
                throw new IllegalArgumentException("配置不存在");
            }
			Assert.isTrue(entity.getRegionId().equals(dto.getRegionId())
					&& entity.getSkuCode().equals(dto.getSkuCode()), "配置维度不允许修改");
        } else {
            entity = baseMapper.selectByRegionAndSku(currentErp(), dto.getRegionId(), dto.getSkuCode());
            if (entity == null) {
                entity = new InventoryConfig();
                entity.setRegionId(dto.getRegionId());
                entity.setSkuCode(dto.getSkuCode());
                entity.setCreateBy(this.principalAttributeAccessor.getUserId());
            }
        }

        entity.setSafetyStock(dto.getSafetyStock());
        entity.setNotifyEnabled(dto.getNotifyEnabled());
        entity.setNotifyThresholdDays(dto.getNotifyThresholdDays());

        this.saveOrUpdate(entity);
    }

    /**
     * 删除 SKU 配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkuConfig(Long id) {
		InventoryConfig entity = baseMapper.selectOne(WrappersX.lambdaQueryX(InventoryConfig.class)
				.eq(InventoryConfig::getId, id)
				.eq(InventoryConfig::getErpTenantId, currentErp() == null ? -1L : currentErp()));
		Assert.notNull(entity, "配置不存在");
		Assert.isTrue(baseMapper.delete(WrappersX.lambdaQueryX(InventoryConfig.class)
				.eq(InventoryConfig::getId, id)
				.eq(InventoryConfig::getErpTenantId, currentErp())) == 1, "配置已变化，请刷新后重试");
    }

	public EffectiveInventoryConfig getEffectiveConfig(Long regionId, String skuCode) {
		InventoryAlertConfig global = getGlobalAlertConfig();
		InventoryConfig sku = baseMapper.selectByRegionAndSku(currentErp(), regionId, skuCode);
		return EffectiveInventoryConfig.builder()
				.safetyStock(sku != null && sku.getSafetyStock() != null ? sku.getSafetyStock() : global.getSafetyStock())
				.notifyEnabled(sku != null && sku.getNotifyEnabled() != null
						? sku.getNotifyEnabled() : global.getNotifyEnabled())
				.notifyThresholdDays(sku != null && sku.getNotifyThresholdDays() != null
						? sku.getNotifyThresholdDays() : global.getNotifyThresholdDays())
				.source(sku == null ? "GLOBAL" : "SKU")
				.build();
	}

    /**
     * 获取 SKU 的有效安全库存（优先 SKU 配置，否则全局配置）
     */
    public Integer getEffectiveSafetyStock(Long regionId, String skuCode) {
		return getEffectiveConfig(regionId, skuCode).getSafetyStock();
    }

    /**
     * 获取有效预警阈值天数
     */
    public Integer getEffectiveThresholdDays(Long regionId, String skuCode) {
		return getEffectiveConfig(regionId, skuCode).getNotifyThresholdDays();
    }

    /**
     * 获取全局预警阈值天数
     */
    public Integer getGlobalThresholdDays() {
        return getGlobalAlertConfig().getNotifyThresholdDays();
    }

    /**
     * 获取全局安全库存
     */
    public Integer getGlobalSafetyStock() {
        return getGlobalAlertConfig().getSafetyStock();
    }

    /**
     * 获取动态有效安全库存
     * <p>
     * effectiveSafetyStock = max(safetyStock, dailySales × thresholdDays)
     *
     * @param regionId   区域 ID
     * @param skuCode    SKU 编码
     * @param dailySales 日均销量
     * @return 动态安全库存
     */
    public int getEffectiveSafetyStockDynamic(Long regionId, String skuCode, int dailySales) {
        int safetyStock = getEffectiveSafetyStock(regionId, skuCode);
        int thresholdDays = getEffectiveThresholdDays(regionId, skuCode);
        return Math.max(safetyStock, dailySales * thresholdDays);
    }

	public List<InventoryConfig> listConfiguredRegionSkus(java.util.Collection<Long> regionIds,
			String skuKeyword) {
		return baseMapper.selectByOwnerAndRegions(currentErp(), regionIds, skuKeyword);
	}
}
