package com.erp.admin.order.service.ozon;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import cn.idev.excel.FastExcel;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.enums.FulfillmentType;
import com.erp.admin.order.model.vo.OzonOrderRejectVO;
import com.erp.admin.order.model.vo.OzonPickListBatchVO;
import com.erp.admin.order.model.vo.OzonPickListFileVO;
import com.erp.admin.order.model.vo.OzonPickListRowVO;
import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.product.model.vo.SkuBriefVO;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Ozon 拣货单服务。
 * <p>
 * 按店铺各生成一份拣货单，供仓库人员照单取货。大仓与小仓订单都需要拣货单，
 * 因此此处<b>不做仓型过滤</b>（仅【准备发运】限定大仓）。
 * <p>
 * <b>输出格式为 Excel 而非 PDF</b>：拣货单需打印俄文商品名，而项目内的 PDFBox
 * 仅内置 Latin-1 字体，渲染西里尔字母会抛异常，需额外内嵌 Unicode 字体文件。
 * 改用项目已有的 FastExcel（{@code ballcat-spring-boot-starter-fastexcel}），
 * Unicode 零成本且可直接打印。
 * <p>
 * SKU 解析走 {@code platform_item_id → sku_mapping → sku_code}，与面单同一条链路。
 * 映射缺失的行不会导致整单失败，而是在「备注」列标注，便于货主回头补映射。
 *
 * @author system
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OzonPickListService {

	private static final String SHEET_NAME = "拣货单";

	private static final String MISSING_MAPPING_REMARK = "SKU 映射缺失";

	private final ErpOrderMapper erpOrderMapper;
	private final ErpOrderItemMapper erpOrderItemMapper;
	private final SkuMappingService skuMappingService;
	private final SkuBriefService skuBriefService;
	private final ShopService shopService;
	private final OssService ossService;

	/**
	 * 生成拣货单：校验订单 → 按店铺分组 → 每组一份 Excel → 上传 OSS。
	 *
	 * @param orderIds 选中的订单ID
	 * @return 批次信息（每店铺一份文件 + 被排除订单的原因）
	 */
	public OzonPickListBatchVO generate(List<Long> orderIds) {
		OzonPickListBatchVO batch = new OzonPickListBatchVO();
		batch.setBatchNo(generateBatchNo());
		if (CollectionUtils.isEmpty(orderIds)) {
			return batch;
		}

		List<ErpOrder> orders = erpOrderMapper.selectBatchIds(orderIds);

		// 1. 逐单校验
		List<ErpOrder> eligible = new ArrayList<>();
		for (ErpOrder order : orders) {
			String reason = rejectReason(order);
			if (reason == null) {
				eligible.add(order);
			}
			else {
				batch.getFailed().add(new OzonOrderRejectVO(order.getId(), order.getPlatformOrderId(), reason));
			}
		}
		if (eligible.isEmpty()) {
			return batch;
		}

		// 2. 批量加载明细，避免 N+1
		List<Long> eligibleIds = eligible.stream().map(ErpOrder::getId).collect(Collectors.toList());
		Map<Long, List<ErpOrderItem>> itemsByOrder = erpOrderItemMapper.selectByOrderIds(eligibleIds).stream()
				.collect(Collectors.groupingBy(ErpOrderItem::getOrderId));

		// 3. platform_item_id → sku_code → SKU 简要信息（品名）
		Set<String> platformItemIds = itemsByOrder.values().stream()
				.flatMap(List::stream)
				.map(ErpOrderItem::getPlatformItemId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		Map<String, String> skuCodeMap = platformItemIds.isEmpty()
				? Collections.emptyMap()
				: skuMappingService.getSkuCodeMapByPlatformItemIds(platformItemIds);
		Set<String> skuCodes = new HashSet<>(skuCodeMap.values());
		Map<String, SkuBriefVO> skuBriefMap = skuCodes.isEmpty()
				? Collections.emptyMap()
				: skuBriefService.buildMapForQuery(skuCodes);

		// 4. 按店铺分组，一店一份
		Map<Long, List<ErpOrder>> byShop = eligible.stream()
				.collect(Collectors.groupingBy(ErpOrder::getShopId, LinkedHashMap::new, Collectors.toList()));

		for (Map.Entry<Long, List<ErpOrder>> entry : byShop.entrySet()) {
			batch.getFiles().add(buildOneFile(batch.getBatchNo(), entry.getKey(), entry.getValue(),
					itemsByOrder, skuCodeMap, skuBriefMap));
		}

		log.info("[OZON][PICK] 拣货单生成完成: batchNo={}, 文件数={}, 排除订单数={}",
				batch.getBatchNo(), batch.getFiles().size(), batch.getFailed().size());
		return batch;
	}

	/** 生成单个店铺的拣货单并上传 OSS。 */
	private OzonPickListFileVO buildOneFile(String batchNo, Long shopId, List<ErpOrder> orders,
			Map<Long, List<ErpOrderItem>> itemsByOrder,
			Map<String, String> skuCodeMap,
			Map<String, SkuBriefVO> skuBriefMap) {

		Shop shop = shopService.getById(shopId);
		String shopName = shop != null ? shop.getName() : ("店铺" + shopId);

		OzonPickListFileVO vo = new OzonPickListFileVO();
		vo.setShopId(shopId);
		vo.setShopName(shopName);
		vo.setOrderCount(orders.size());

		List<OzonPickListRowVO> rows = new ArrayList<>();
		Set<String> distinctSkus = new HashSet<>();
		int seq = 0;

		for (ErpOrder order : orders) {
			for (ErpOrderItem item : itemsByOrder.getOrDefault(order.getId(), Collections.emptyList())) {
				String skuCode = skuCodeMap.get(item.getPlatformItemId());
				// 映射缺失时回退展示平台货号，并在备注列标注，避免整单不可拣
				boolean mapped = StringUtils.hasText(skuCode);
				SkuBriefVO brief = mapped ? skuBriefMap.get(skuCode) : null;

				if (mapped) {
					distinctSkus.add(skuCode);
				}

				rows.add(new OzonPickListRowVO(
						++seq,
						order.getPlatformOrderId(),
						order.getShipmentId(),
						mapped ? skuCode : item.getPlatformItemId(),
						brief != null ? brief.getSkuName() : null,
						item.getQuantity(),
						mapped ? null : MISSING_MAPPING_REMARK));
			}
		}
		vo.setSkuCount(distinctSkus.size());

		try {
			byte[] excel = writeExcel(rows);
			String fileName = String.format("%s-拣货单-%s.xlsx", sanitize(shopName),
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			String objectKey = LabelConstants.OSS_LABEL_PATH_PREFIX + batchNo + "/" + fileName;
			ossService.putObject(OssBucketKeys.PUBLIC_FILES, excel, objectKey);

			vo.setFileName(fileName);
			vo.setObjectKey(objectKey);
			vo.setDownloadUrl(ossService.getDownloadUrl(OssBucketKeys.PUBLIC_FILES, objectKey));
			log.info("[OZON][PICK] 拣货单已生成: shopId={}, rows={}, objectKey={}", shopId, rows.size(), objectKey);
		}
		catch (Exception e) {
			vo.setErrorMsg("生成失败: " + e.getMessage());
			log.error("[OZON][PICK] 拣货单生成失败: shopId={}, error={}", shopId, e.getMessage(), e);
		}
		return vo;
	}

	/** 用 FastExcel 把行写成 xlsx 字节。 */
	private byte[] writeExcel(List<OzonPickListRowVO> rows) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			FastExcel.write(out, OzonPickListRowVO.class).sheet(SHEET_NAME).doWrite(rows);
			return out.toByteArray();
		}
		catch (Exception e) {
			throw new IllegalStateException("写出 Excel 失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 拣货单准入校验：Ozon + FBS + 未锁定 + 已发货。
	 * 与前端 OzonOrderPickListDialog.canPick() 保持一致。不区分大小仓。
	 *
	 * @return null 表示可拣货；否则返回给货主看的原因
	 */
	private String rejectReason(ErpOrder order) {
		if (!PlatformEnum.Ozon.code().equalsIgnoreCase(order.getPlatform())) {
			return "非 Ozon 平台";
		}
		if (!FulfillmentType.FBS.matches(order.getFulfillmentType())) {
			return "FBO 订单不支持拣货单";
		}
		if (order.getLocked() != null && order.getLocked() == 1) {
			return "订单已锁定";
		}
		if (!"SHIPPED".equals(order.getErpStatus())) {
			return "状态不支持：" + (order.getErpStatus() == null ? "-" : order.getErpStatus());
		}
		return null;
	}

	/** 文件名安全化：去掉路径分隔符等不适合做 OSS key 的字符 */
	private String sanitize(String name) {
		if (!StringUtils.hasText(name)) {
			return "shop";
		}
		return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
	}

	private String generateBatchNo() {
		return "PICK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
	}
}
