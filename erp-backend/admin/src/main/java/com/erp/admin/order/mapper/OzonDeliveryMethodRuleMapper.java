package com.erp.admin.order.mapper;

import com.erp.admin.order.model.entity.OzonDeliveryMethodRule;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

public interface OzonDeliveryMethodRuleMapper extends ExtendMapper<OzonDeliveryMethodRule> {

	default OzonDeliveryMethodRule selectEnabled(Long shopId, Long deliveryMethodId) {
		return selectOne(WrappersX.<OzonDeliveryMethodRule>lambdaQueryX()
				.eq(OzonDeliveryMethodRule::getShopId, shopId)
				.eq(OzonDeliveryMethodRule::getDeliveryMethodId, deliveryMethodId)
				.eq(OzonDeliveryMethodRule::getEnabled, 1));
	}

	default List<OzonDeliveryMethodRule> selectAllRules() {
		return selectList(WrappersX.<OzonDeliveryMethodRule>lambdaQueryX()
				.orderByAsc(OzonDeliveryMethodRule::getShopId, OzonDeliveryMethodRule::getDeliveryMethodId));
	}
}
