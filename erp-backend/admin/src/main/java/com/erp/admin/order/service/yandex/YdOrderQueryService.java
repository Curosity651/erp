package com.erp.admin.order.service.yandex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.order.converter.ErpOrderConverter;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.order.model.vo.YdOrderPageVO;
import com.erp.admin.order.service.common.OrderQueryHelper;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.product.enums.ImageScene;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Yandex 订单查询服务
 * <p>
 * 遵循 WB/Ozon 三阶段填充模式：
 * 1. MapStruct 转换 ErpOrder → YdOrderPageVO
 * 2. 填充店铺名称
 * 3. 批量加载 items + SKU 元数据
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YdOrderQueryService {

	private final ErpOrderMapper orderMapper;
	private final ErpOrderItemMapper orderItemMapper;
	private final OrderQueryHelper queryHelper;
	private final SkuBriefService skuBriefService;
	private final SkuMappingService skuMappingService;

	/**
	 * 分页查询 Yandex 订单
	 */
	public PageResult<YdOrderPageVO> queryPage(PageParam pageParam, ErpOrderQO qo) {
		ErpOrderQO processedQo = queryHelper.preprocess(qo, PlatformEnum.Yandex.code());
		if (processedQo == null) {
			return new PageResult<>();
		}

		PageResult<ErpOrder> entityPage = orderMapper.queryPageEntities(pageParam, processedQo);
		if (entityPage == null || CollectionUtils.isEmpty(entityPage.getRecords())) {
			return new PageResult<>();
		}

		List<YdOrderPageVO> voList = convertToYdOrderPageVO(entityPage.getRecords(), ImageScene.QUERY);
		return new PageResult<>(voList, entityPage.getTotal());
	}

	// ==================== 内部方法 ====================

	private List<YdOrderPageVO> convertToYdOrderPageVO(List<ErpOrder> entities, ImageScene scene) {
		List<YdOrderPageVO> voList = new ArrayList<>(entities.size());
		for (ErpOrder entity : entities) {
			YdOrderPageVO vo = ErpOrderConverter.INSTANCE.poToYdPageVo(entity);
			voList.add(vo);
		}

		// 填充店铺名称
		queryHelper.enrichShopName(voList, YdOrderPageVO::getShopId, YdOrderPageVO::setErpShopName);

		// 批量加载 items 并填充 SKU 展示信息
		enrichItems(voList, scene);

		return voList;
	}

	/**
	 * 批量加载订单商品明细并填充 SKU 展示信息
	 */
	private void enrichItems(List<YdOrderPageVO> voList, ImageScene scene) {
		// 1. 批量加载 items
		List<Long> orderIds = voList.stream().map(YdOrderPageVO::getId).collect(Collectors.toList());
		List<ErpOrderItem> allItems = orderItemMapper.selectByOrderIds(orderIds);
		Map<Long, List<ErpOrderItem>> itemMap = allItems.stream()
				.collect(Collectors.groupingBy(ErpOrderItem::getOrderId));

		// 2. 通过 SkuMappingService 实时查询 skuCode
		Set<String> platformItemIds = allItems.stream()
				.map(ErpOrderItem::getPlatformItemId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		Map<String, String> skuCodeMap = skuMappingService
				.getSkuCodeMapByPlatformItemIds(platformItemIds);
		Set<String> allSkuCodes = new HashSet<>(skuCodeMap.values());
		Map<String, SkuBriefVO> skuBriefMap = (scene == ImageScene.EXPORT)
				? skuBriefService.buildMapForExport(allSkuCodes)
				: skuBriefService.buildMapForQuery(allSkuCodes);

		// 3. 组装 items 到 VO
		for (YdOrderPageVO vo : voList) {
			List<ErpOrderItem> items = itemMap.getOrDefault(vo.getId(), Collections.emptyList());
			List<OrderItemVO> itemVOs = items.stream().map(item -> {
				OrderItemVO itemVO = new OrderItemVO();
				itemVO.setPlatformItemId(item.getPlatformItemId());
				String skuCode = skuCodeMap.get(item.getPlatformItemId());
				itemVO.setSkuCode(skuCode);
				itemVO.setQuantity(item.getQuantity());
				itemVO.setItemPrice(item.getItemPrice());
				itemVO.setItemAmount(item.getItemAmount());
				if (StringUtils.hasText(skuCode)) {
					SkuBriefVO brief = skuBriefMap.get(skuCode);
					if (brief != null) {
						itemVO.setSkuName(brief.getSkuName());
						itemVO.setMainImage(brief.getMainImage());
					}
				}
				return itemVO;
			}).collect(Collectors.toList());
			vo.setItems(itemVOs);
		}
	}
}
