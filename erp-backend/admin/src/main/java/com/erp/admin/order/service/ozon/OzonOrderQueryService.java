package com.erp.admin.order.service.ozon;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.common.util.CurrencyUtils;
import com.erp.admin.order.converter.ErpOrderConverter;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.OwnerOrderBusinessStatus;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.order.model.vo.OzonOrderExportVO;
import com.erp.admin.order.model.vo.OzonOrderPageVO;
import com.erp.admin.order.service.common.OrderExportHelper;
import com.erp.admin.order.service.common.OrderQueryHelper;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.ozon.enums.OzonStatusEnum;
import com.erp.admin.platform.ozon.model.response.posting.OzonDeliveryMethod;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.product.enums.ImageScene;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Ozon 订单查询 + 导出服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonOrderQueryService {

	private final ErpOrderMapper orderMapper;
	private final OrderQueryHelper queryHelper;
	private final OrderExportHelper exportHelper;
	private final ObjectMapper objectMapper;
	private final ErpOrderItemMapper orderItemMapper;
	private final SkuBriefService skuBriefService;
	private final SkuMappingService skuMappingService;

	/**
	 * 分页查询
	 */
	public PageResult<OzonOrderPageVO> queryPage(PageParam pageParam, ErpOrderQO qo) {
		ErpOrderQO processedQo = queryHelper.preprocess(qo, PlatformEnum.Ozon.code());
		if (processedQo == null) {
			return new PageResult<>();
		}

		PageResult<ErpOrder> entityPage = orderMapper.queryPageEntities(pageParam, processedQo);
		if (entityPage == null || CollectionUtils.isEmpty(entityPage.getRecords())) {
			return new PageResult<>();
		}

		List<OzonOrderPageVO> voList = convertToOzonOrderPageVO(entityPage.getRecords(), ImageScene.QUERY);
		return new PageResult<>(voList, entityPage.getTotal());
	}

	/**
	 * 分批查询导出数据
	 */
	public List<OzonOrderExportVO> queryExportBatch(ErpOrderQO qo, int currentPage, int pageSize) {
		ErpOrderQO processedQo = queryHelper.preprocess(qo, PlatformEnum.Ozon.code());
		if (processedQo == null) {
			return Collections.emptyList();
		}

		List<ErpOrder> orders = exportHelper.queryExportBatch(processedQo, currentPage, pageSize);
		if (orders.isEmpty()) {
			return Collections.emptyList();
		}

		List<OzonOrderPageVO> pageVOs = convertToOzonOrderPageVO(orders, ImageScene.EXPORT);
		if (CollectionUtils.isEmpty(pageVOs)) {
			return Collections.emptyList();
		}

		return pageVOs.stream().flatMap(vo -> convertPageVOToExportVOs(vo).stream()).collect(Collectors.toList());
	}

	// ==================== 内部方法 ====================

	private List<OzonOrderPageVO> convertToOzonOrderPageVO(List<ErpOrder> entities, ImageScene scene) {
		List<OzonOrderPageVO> voList = new ArrayList<>(entities.size());
		for (ErpOrder entity : entities) {
			OzonOrderPageVO vo = ErpOrderConverter.INSTANCE.poToOzonPageVo(entity);
			vo.setBusinessStatus(OwnerOrderBusinessStatus.resolve(entity).name());
			parseAndFillOzonInfo(vo, entity);
			voList.add(vo);
		}

		queryHelper.enrichShopName(voList, OzonOrderPageVO::getShopId, OzonOrderPageVO::setErpShopName);

		// 批量加载 items 并填充 SKU 展示信息
		enrichItems(voList, scene);

		return voList;
	}

	private void parseAndFillOzonInfo(OzonOrderPageVO vo, ErpOrder entity) {
		if (!StringUtils.hasText(entity.getRawJson())) {
			return;
		}
		try {
			OzonPosting posting = objectMapper.readValue(entity.getRawJson(), OzonPosting.class);

			// 物流方式与发货仓库（仅 FBS 订单的 rawJson 含 delivery_method）
			OzonDeliveryMethod dm = posting.getDeliveryMethod();
			if (dm != null) {
				// 仓库名只用于前端展示及仓型标签；交接单要求由店铺与配送方式规则决定。
				vo.setWarehouseName(dm.getWarehouse());
				// 物流方式 ID 是生成运单(act)的分组键与入参
				vo.setDeliveryMethodId(dm.getId());
				vo.setDeliveryMethodName(dm.getName());
				vo.setTplProviderId(dm.getTplProviderId());
				vo.setTplProviderName(dm.getTplProvider());
			}
		} catch (Exception e) {
			log.warn("[OZON] rawJson 解析失败 orderId={}: {}", entity.getId(), e.getMessage());
		}
	}

	/**
	 * 批量加载订单商品明细并填充 SKU 展示信息
	 */
	private void enrichItems(List<OzonOrderPageVO> voList, ImageScene scene) {
		// 1. 批量加载 items
		List<Long> orderIds = voList.stream().map(OzonOrderPageVO::getId).collect(Collectors.toList());
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
		for (OzonOrderPageVO vo : voList) {
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

	/**
	 * 将 PageVO 转为 ExportVO 列表（多商品订单一个 item 一行）
	 */
	private List<OzonOrderExportVO> convertPageVOToExportVOs(OzonOrderPageVO pageVO) {
		List<OrderItemVO> items = pageVO.getItems();
		if (items == null || items.isEmpty()) {
			return Collections.singletonList(buildBaseExportVO(pageVO));
		}
		List<OzonOrderExportVO> result = new ArrayList<>();
		for (OrderItemVO item : items) {
			OzonOrderExportVO exportVO = buildBaseExportVO(pageVO);
			exportVO.setSkuCode(item.getSkuCode());
			if (StringUtils.hasText(item.getMainImage())) {
				try {
					exportVO.setSkuImage(new URL(item.getMainImage()));
				} catch (Exception e) {
					log.warn("[OZON][EXPORT] 图片URL解析失败: {}", item.getMainImage());
				}
			}
			result.add(exportVO);
		}
		return result;
	}

	private OzonOrderExportVO buildBaseExportVO(OzonOrderPageVO pageVO) {
		OzonOrderExportVO exportVO = new OzonOrderExportVO();
		exportVO.setPlatform(pageVO.getPlatform());
		exportVO.setErpShopName(pageVO.getErpShopName());
		exportVO.setPlatformOrderId(pageVO.getPlatformOrderId());
		if (pageVO.getConvertedAmount() != null) {
			exportVO.setTotalAmountCny(CurrencyUtils.formatCurrency(pageVO.getConvertedAmount(), "CNY"));
		}
		exportVO.setPlatformStatusText(mapOzonStatusLabel(pageVO.getPlatformStatus()));
		String deliveryType = null;
		if ("FBO".equalsIgnoreCase(pageVO.getFulfillmentType())) {
			deliveryType = "Ozon 仓配";
		} else if ("FBS".equalsIgnoreCase(pageVO.getFulfillmentType())) {
			deliveryType = pageVO.getTplProviderName() != null ? pageVO.getTplProviderName() : "FBS";
		}
		exportVO.setDeliveryMethodName(deliveryType);
		exportVO.setFulfillmentType(pageVO.getFulfillmentType());
		exportVO.setPlatformCreatedAtMoscow(pageVO.getPlatformCreatedAtMoscow());
		return exportVO;
	}

	private String mapOzonStatusLabel(String platformStatus) {
		if (platformStatus == null) return "-";
		String label = OzonStatusEnum.getLabelByCode(platformStatus);
		return label != null ? label : platformStatus;
	}

}
