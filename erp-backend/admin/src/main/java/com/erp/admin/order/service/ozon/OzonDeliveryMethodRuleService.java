package com.erp.admin.order.service.ozon;

import com.erp.admin.order.mapper.OzonDeliveryMethodRuleMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.OzonDeliveryMethodRule;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.shop.service.ShopService;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OzonDeliveryMethodRuleService {

	private final OzonDeliveryMethodRuleMapper ruleMapper;
	private final ErpOrderMapper erpOrderMapper;
	private final ShopService shopService;

	public OzonDeliveryMethodRule find(ErpOrder order) {
		if (order == null || order.getShopId() == null || order.getDeliveryMethodId() == null) {
			return null;
		}
		return ruleMapper.selectEnabled(order.getShopId(), order.getDeliveryMethodId());
	}

	public boolean isActRequired(ErpOrder order) {
		OzonDeliveryMethodRule rule = find(order);
		return rule != null && Integer.valueOf(1).equals(rule.getActRequired());
	}

	public int containersCount(ErpOrder order) {
		OzonDeliveryMethodRule rule = find(order);
		return rule == null || rule.getContainersCount() == null || rule.getContainersCount() < 1
				? 1 : rule.getContainersCount();
	}

	public List<OzonDeliveryMethodRule> list() {
		Map<String, OzonDeliveryMethodRule> result = new LinkedHashMap<>();
		for (OzonDeliveryMethodRule rule : ruleMapper.selectAllRules()) {
			result.put(key(rule.getShopId(), rule.getDeliveryMethodId()), rule);
		}
		List<ErpOrder> observed = erpOrderMapper.selectList(WrappersX.<ErpOrder>lambdaQueryX()
				.eq(ErpOrder::getPlatform, "ozon")
				.isNotNull(ErpOrder::getDeliveryMethodId)
				.orderByDesc(ErpOrder::getUpdateTime));
		for (ErpOrder order : observed) {
			String key = key(order.getShopId(), order.getDeliveryMethodId());
			OzonDeliveryMethodRule rule = result.computeIfAbsent(key, ignored -> {
				OzonDeliveryMethodRule created = new OzonDeliveryMethodRule();
				created.setShopId(order.getShopId());
				created.setDeliveryMethodId(order.getDeliveryMethodId());
				created.setActRequired(0);
				created.setContainersCount(1);
				created.setEnabled(1);
				return created;
			});
			if (rule.getDeliveryMethodName() == null) {
				rule.setDeliveryMethodName(order.getDeliveryMethodName());
			}
		}
		for (OzonDeliveryMethodRule rule : result.values()) {
			if (rule.getShopId() != null && shopService.getById(rule.getShopId()) != null) {
				rule.setShopName(shopService.getById(rule.getShopId()).getName());
			}
		}
		return new java.util.ArrayList<>(result.values());
	}

	private String key(Long shopId, Long deliveryMethodId) {
		return shopId + ":" + deliveryMethodId;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long save(OzonDeliveryMethodRule rule) {
		Assert.notNull(rule.getShopId(), "店铺不能为空");
		Assert.notNull(rule.getDeliveryMethodId(), "配送方式不能为空");
		Assert.notNull(shopService.getById(rule.getShopId()), "店铺不存在或不属于当前货主");
		Long observedCount = erpOrderMapper.selectCount(WrappersX.<ErpOrder>lambdaQueryX()
				.eq(ErpOrder::getPlatform, "ozon")
				.eq(ErpOrder::getShopId, rule.getShopId())
				.eq(ErpOrder::getDeliveryMethodId, rule.getDeliveryMethodId()));
		Assert.isTrue(observedCount != null && observedCount > 0, "当前店铺尚未同步到该配送方式");
		rule.setActRequired(Integer.valueOf(1).equals(rule.getActRequired()) ? 1 : 0);
		rule.setContainersCount(rule.getContainersCount() == null || rule.getContainersCount() < 1
				? 1 : rule.getContainersCount());
		rule.setEnabled(rule.getEnabled() == null ? 1 : rule.getEnabled());
		if (rule.getId() == null) {
			ruleMapper.insert(rule);
		}
		else {
			Assert.isTrue(ruleMapper.updateById(rule) == 1, "配送方式规则不存在或已变化");
		}
		return rule.getId();
	}
}
