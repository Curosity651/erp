package com.erp.admin.order.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.erp.admin.order.model.entity.WbSupply;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * WB 发货批次表
 *
 * @author erp 2025-09-27 23:16:08
 */
public interface WbSupplyMapper extends ExtendMapper<WbSupply> {
	default List<WbSupply> selectByShopPlatformAndSupplyIds(Long shopId, String platform, Collection<String> supplyIds) {
		if (supplyIds == null || supplyIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapperX<WbSupply> wrapper = WrappersX.lambdaQueryX(WbSupply.class)
				.eq(WbSupply::getShopId, shopId)
				.eq(WbSupply::getPlatform, platform)
				.in(WbSupply::getSupplyId, supplyIds);
		return this.selectList(wrapper);
	}

	default WbSupply selectOneByShopPlatformSupplyId(Long shopId, String platform, String supplyId) {
		LambdaQueryWrapperX<WbSupply> wrapper = WrappersX.lambdaQueryX(WbSupply.class)
				.eq(WbSupply::getShopId, shopId)
				.eq(WbSupply::getPlatform, platform)
				.eq(WbSupply::getSupplyId, supplyId);
		return this.selectOne(wrapper);
	}

	default List<WbSupply> selectByPlatformAndSupplyIds(String platform, Collection<String> supplyIds) {
		if (supplyIds == null || supplyIds.isEmpty()) return Collections.emptyList();
		LambdaQueryWrapperX<WbSupply> wrapper = WrappersX.lambdaQueryX(WbSupply.class)
				.eq(WbSupply::getPlatform, platform)
				.in(WbSupply::getSupplyId, supplyIds);
		return this.selectList(wrapper);
	}

    default void updateLabelBase64(Long supplyId, String labelBase64) {
		LambdaUpdateWrapper<WbSupply> updateWrapper = WrappersX.lambdaUpdate(WbSupply.class)
				.set(WbSupply::getLabelBase64, labelBase64)
				.eq(WbSupply::getId, supplyId);
		this.update(null, updateWrapper);
	}

}