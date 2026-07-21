package com.erp.admin.order.service.wildberries;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.common.util.CurrencyUtils;
import com.erp.admin.order.converter.ErpOrderConverter;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.mapper.WbOfficeMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.entity.WbOffice;
import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.order.model.vo.OrderItemVO;
import com.erp.admin.order.model.vo.WbOrderExportVO;
import com.erp.admin.order.model.vo.WbOrderPageVO;
import com.erp.admin.order.service.common.OrderExportHelper;
import com.erp.admin.order.service.common.OrderQueryHelper;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.wildberries.enums.WildberriesWbStatusEnum;
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
 * Wildberries 订单查询 + 导出服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOrderQueryService {

	private final ErpOrderMapper orderMapper;
	private final WbOfficeMapper wbOfficeMapper;
	private final OrderQueryHelper queryHelper;
	private final OrderExportHelper exportHelper;
	private final ErpOrderItemMapper orderItemMapper;
	private final SkuBriefService skuBriefService;
	private final SkuMappingService skuMappingService;

	/**
	 * 分页查询
	 */
	public PageResult<WbOrderPageVO> queryPage(PageParam pageParam, ErpOrderQO qo) {
		ErpOrderQO processedQo = queryHelper.preprocess(qo, PlatformEnum.Wildberries.code());
		if (processedQo == null) {
			return new PageResult<>();
		}

		PageResult<ErpOrder> entityPage = orderMapper.queryPageEntities(pageParam, processedQo);
		if (entityPage == null || CollectionUtils.isEmpty(entityPage.getRecords())) {
			return new PageResult<>();
		}

		List<WbOrderPageVO> voList = convertToWbOrderPageVO(entityPage.getRecords(), ImageScene.QUERY);
		return new PageResult<>(voList, entityPage.getTotal());
	}

	/**
	 * 分批查询导出数据
	 */
	public List<WbOrderExportVO> queryExportBatch(ErpOrderQO qo, int currentPage, int pageSize) {
		ErpOrderQO processedQo = queryHelper.preprocess(qo, PlatformEnum.Wildberries.code());
		if (processedQo == null) {
			return Collections.emptyList();
		}

		List<ErpOrder> orders = exportHelper.queryExportBatch(processedQo, currentPage, pageSize);
		if (orders.isEmpty()) {
			return Collections.emptyList();
		}

		List<WbOrderPageVO> pageVOs = convertToWbOrderPageVO(orders, ImageScene.EXPORT);
		if (CollectionUtils.isEmpty(pageVOs)) {
			return Collections.emptyList();
		}

		return pageVOs.stream().flatMap(vo -> convertPageVOToExportVOs(vo).stream()).collect(Collectors.toList());
	}

	// ==================== 内部方法 ====================

	private List<WbOrderPageVO> convertToWbOrderPageVO(List<ErpOrder> entities, ImageScene scene) {
		List<WbOrderPageVO> voList = new ArrayList<>(entities.size());
		for (ErpOrder entity : entities) {
			WbOrderPageVO vo = ErpOrderConverter.INSTANCE.poToWbPageVo(entity);
			voList.add(vo);
		}

		enrichWarehouseInfoForWb(voList);
		queryHelper.enrichShopName(voList, WbOrderPageVO::getShopId, WbOrderPageVO::setErpShopName);

		// 批量加载 items 并填充 SKU 展示信息
		enrichItems(voList, scene);

		return voList;
	}

	private void enrichWarehouseInfoForWb(List<WbOrderPageVO> voList) {
		Set<Long> officeIds = new HashSet<>();
		Set<Long> shopIds = new HashSet<>();
		for (WbOrderPageVO vo : voList) {
			if (StringUtils.hasText(vo.getDestinationWarehouseId())) {
				try {
					officeIds.add(Long.parseLong(vo.getDestinationWarehouseId()));
					if (vo.getShopId() != null) {
						shopIds.add(vo.getShopId());
					}
				} catch (NumberFormatException ignore) {
				}
			}
		}
		if (officeIds.isEmpty() || shopIds.isEmpty()) {
			return;
		}
		List<WbOffice> offices = wbOfficeMapper.selectList(Wrappers.lambdaQuery(WbOffice.class)
				.in(WbOffice::getOfficeId, officeIds)
				.in(WbOffice::getShopId, shopIds));
		Map<String, WbOffice> keyToOffice = new HashMap<>();
		for (WbOffice o : offices) {
			keyToOffice.put(o.getShopId() + "#" + o.getOfficeId(), o);
		}
		for (WbOrderPageVO vo : voList) {
			if (StringUtils.hasText(vo.getDestinationWarehouseId()) && vo.getShopId() != null) {
				String key = vo.getShopId() + "#" + vo.getDestinationWarehouseId();
				WbOffice o = keyToOffice.get(key);
				if (o != null) {
					vo.setDestinationWarehouseName(o.getName());
					vo.setDestinationWarehouseAddress(o.getAddress());
				}
			}
		}
	}

	/**
	 * 批量加载订单商品明细并填充 SKU 展示信息
	 */
	private void enrichItems(List<WbOrderPageVO> voList, ImageScene scene) {
		// 1. 批量加载 items
		List<Long> orderIds = voList.stream().map(WbOrderPageVO::getId).collect(Collectors.toList());
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
		for (WbOrderPageVO vo : voList) {
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
	private List<WbOrderExportVO> convertPageVOToExportVOs(WbOrderPageVO pageVO) {
		List<OrderItemVO> items = pageVO.getItems();
		if (items == null || items.isEmpty()) {
			return Collections.singletonList(buildBaseExportVO(pageVO));
		}
		List<WbOrderExportVO> result = new ArrayList<>();
		for (OrderItemVO item : items) {
			WbOrderExportVO exportVO = buildBaseExportVO(pageVO);
			exportVO.setSkuCode(item.getSkuCode());
			if (StringUtils.hasText(item.getMainImage())) {
				try {
					exportVO.setSkuImage(new URL(item.getMainImage()));
				} catch (Exception e) {
					log.warn("[WB][EXPORT] 图片URL解析失败: {}", item.getMainImage());
				}
			}
			result.add(exportVO);
		}
		return result;
	}

	private WbOrderExportVO buildBaseExportVO(WbOrderPageVO pageVO) {
		WbOrderExportVO exportVO = new WbOrderExportVO();
		exportVO.setPlatform(pageVO.getPlatform());
		exportVO.setErpShopName(pageVO.getErpShopName());
		exportVO.setPlatformOrderId(pageVO.getPlatformOrderId());
		if (pageVO.getConvertedAmount() != null) {
			exportVO.setTotalAmountCny(CurrencyUtils.formatCurrency(pageVO.getConvertedAmount(), "CNY"));
		}
		exportVO.setPlatformStatusLabel(mapWbStatusLabel(pageVO.getPlatformStatus()));
		exportVO.setDestinationWarehouseName(pageVO.getDestinationWarehouseName());
		exportVO.setDestinationWarehouseAddress(pageVO.getDestinationWarehouseAddress());
		exportVO.setLabelStatus(buildOperationStatus(pageVO));
		exportVO.setPlatformCreatedAtMoscow(pageVO.getPlatformCreatedAtMoscow());
		return exportVO;
	}

	private String mapWbStatusLabel(String platformStatus) {
		if (platformStatus == null) return "-";
		String label = WildberriesWbStatusEnum.getLabelByCode(platformStatus);
		return label != null ? label : platformStatus;
	}

	private String buildOperationStatus(WbOrderPageVO pageVO) {
		List<String> statuses = new ArrayList<>();
		if (pageVO.getLocked() != null && pageVO.getLocked() == 1) {
			statuses.add("已锁定");
		}
		if (pageVO.getHasLabel() != null && pageVO.getHasLabel()) {
			statuses.add("面单已获取");
		} else {
			statuses.add("面单待获取");
		}
		return String.join(", ", statuses);
	}

}
