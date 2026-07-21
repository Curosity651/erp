package com.erp.admin.order.service.ozon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.mapper.OzonShipmentActMapper;
import com.erp.admin.order.mapper.OzonShipmentActOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.OzonShipmentAct;
import com.erp.admin.order.model.entity.OzonShipmentActOrder;
import com.erp.admin.order.model.enums.FulfillmentType;
import com.erp.admin.order.model.vo.OzonActBatchVO;
import com.erp.admin.order.model.vo.OzonActVO;
import com.erp.admin.order.model.vo.OzonOrderRejectVO;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.response.posting.OzonDeliveryMethod;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.service.OssService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Ozon 运单（交接单 act）服务。
 * <p>
 * <b>业务定位</b>：仅大仓（仓库名含「大」字）的 Ozon FBS 订单在打完面单后需要生成运单。
 * 小仓订单打完面单与拣货单后流程即结束。
 * <p>
 * <b>关键语义</b>：运单由 Ozon 按「物流方式 + 发货日期」汇总当日全部待发货件生成，
 * 其 PDF 内容<b>不等于</b>调用方选中的订单集合。选中订单只用于推导出应向哪几个
 * (店铺, 物流方式) 创建运单，以及留痕。
 * <p>
 * <b>异步模型</b>：Ozon 侧生成 PDF 约需 1~2 分钟。本服务不起后台线程，
 * {@link #createActs} 立即返回批次号，由前端轮询 {@link #pollBatch}；
 * 每次轮询只对 PENDING 的运单查一次状态，就绪则当场取 PDF 并落 OSS。
 * 这样既避免长 HTTP 请求被网关超时切断，也规避了线程池复用导致
 * {@code TenantContext}（InheritableThreadLocal）不传播而跨租户串数据的风险。
 *
 * @author system
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OzonActService {

	/** Ozon 返回的就绪状态（大小写不敏感）。官方 OpenAPI 未收录 act 接口，取值以实测为准。 */
	private static final List<String> READY_STATUSES = java.util.Arrays.asList("ready", "formed");

	/** Ozon 返回的失败状态（大小写不敏感）。其余未知状态一律视为「仍在生成」，继续轮询。 */
	private static final List<String> FAILED_STATUSES = java.util.Arrays.asList("error", "failed", "cancelled");

	/** 大仓关键字：仓库名含该字即为大仓（大件），只有大仓需要生成运单。 */
	private static final String BIG_WAREHOUSE_KEYWORD = "大";

	private static final int DEFAULT_CONTAINERS_COUNT = 1;

	private final ErpOrderMapper erpOrderMapper;
	private final OzonShipmentActMapper actMapper;
	private final OzonShipmentActOrderMapper actOrderMapper;
	private final OzonPlatformApi ozonPlatformApi;
	private final ShopService shopService;
	private final CredentialService credentialService;
	private final LabelService labelService;
	private final OssService ossService;
	private final ObjectMapper objectMapper;

	// ==================================================================
	// 创建运单
	// ==================================================================

	/**
	 * 【准备发运】：校验订单 → 按 (店铺, 物流方式) 分组 → 每组创建一份运单。
	 * <p>
	 * 立即返回；运单 PDF 由前端轮询 {@link #pollBatch} 获取。
	 *
	 * @param orderIds      选中的订单ID
	 * @param departureDate 发货日期
	 * @param userId        操作人
	 * @return 批次信息（含各运单初始状态与被排除订单的原因）
	 */
	public OzonActBatchVO createActs(List<Long> orderIds, LocalDate departureDate, Long userId) {
		OzonActBatchVO batch = new OzonActBatchVO();
		if (CollectionUtils.isEmpty(orderIds) || departureDate == null) {
			batch.setBatchNo(generateBatchNo());
			return batch;
		}

		List<ErpOrder> orders = erpOrderMapper.selectBatchIds(orderIds);

		// 1. 逐单校验，不合格的带原因返回
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

		String batchNo = generateBatchNo();
		batch.setBatchNo(batchNo);
		if (eligible.isEmpty()) {
			return batch;
		}

		// 2. 按 (店铺, 物流方式) 分组——运单只能用单一店铺的凭证，且物流方式是 act 的入参
		Map<String, List<ErpOrder>> groups = eligible.stream()
				.collect(Collectors.groupingBy(o -> o.getShopId() + "__" + deliveryMethodIdOf(o),
						LinkedHashMap::new, Collectors.toList()));

		// 3. 逐组创建
		for (List<ErpOrder> group : groups.values()) {
			ErpOrder sample = group.get(0);
			OzonShipmentAct act = createOneAct(batchNo, sample, group, departureDate, userId);
			batch.getActs().add(toVO(act, sample.getShopId()));
		}

		log.info("[OZON][ACT] 准备发运完成: batchNo={}, 运单数={}, 排除订单数={}",
				batchNo, batch.getActs().size(), batch.getFailed().size());
		return batch;
	}

	/**
	 * 创建单份运单。HTTP 调用置于事务之外，各步骤独立落库，失败只影响本组。
	 */
	private OzonShipmentAct createOneAct(String batchNo, ErpOrder sample, List<ErpOrder> group,
			LocalDate departureDate, Long userId) {

		Long shopId = sample.getShopId();
		Long deliveryMethodId = deliveryMethodIdOf(sample);

		// 幂等：同 (店铺, 物流方式, 发货日期) 已有未失败的运单则复用，避免在 Ozon 侧重复创建
		OzonShipmentAct existing = actMapper.selectReusable(shopId, deliveryMethodId, departureDate);
		if (existing != null) {
			log.info("[OZON][ACT] 复用已存在运单: actId={}, ozonActId={}, status={}",
					existing.getId(), existing.getOzonActId(), existing.getStatus());
			linkOrders(existing.getId(), group);
			return existing;
		}

		OzonDeliveryMethod dm = deliveryMethodOf(sample);

		OzonShipmentAct act = new OzonShipmentAct();
		act.setBatchNo(batchNo);
		act.setShopId(shopId);
		act.setDeliveryMethodId(deliveryMethodId);
		act.setDeliveryMethodName(dm != null ? dm.getName() : null);
		act.setWarehouseName(sample.getWarehouseName());
		act.setDepartureDate(departureDate);
		act.setContainersCount(DEFAULT_CONTAINERS_COUNT);
		act.setStatus(OzonShipmentAct.STATUS_CREATING);
		act.setOrderCount(group.size());
		act.setCreatedBy(userId);
		actMapper.insert(act);

		linkOrders(act.getId(), group);

		try {
			Shop shop = shopService.getById(shopId);
			if (shop == null) {
				throw new IllegalStateException("店铺不存在: " + shopId);
			}
			OzonCredential credential = credentialService.parseCredential(shop);

			Long ozonActId = ozonPlatformApi.createAct(credential, deliveryMethodId, departureDate,
					DEFAULT_CONTAINERS_COUNT);

			act.setOzonActId(ozonActId);
			act.setStatus(OzonShipmentAct.STATUS_PENDING);
			actMapper.updateById(act);

			log.info("[OZON][ACT] 创建运单: actId={}, ozonActId={}, shopId={}, deliveryMethodId={}",
					act.getId(), ozonActId, shopId, deliveryMethodId);
		}
		catch (Exception e) {
			act.setStatus(OzonShipmentAct.STATUS_FAILED);
			act.setErrorMsg(truncate(resolveFailReason(e)));
			actMapper.updateById(act);
			log.error("[OZON][ACT] 创建运单失败: shopId={}, deliveryMethodId={}, error={}",
					shopId, deliveryMethodId, e.getMessage(), e);
		}
		return act;
	}

	/** 记录本次纳入的订单（留痕）。唯一键保证重复点击不会写重。 */
	private void linkOrders(Long actId, List<ErpOrder> group) {
		for (ErpOrder order : group) {
			try {
				OzonShipmentActOrder link = new OzonShipmentActOrder();
				link.setActId(actId);
				link.setOrderId(order.getId());
				link.setPostingNumber(order.getShipmentId());
				actOrderMapper.insert(link);
			}
			catch (Exception e) {
				// uk_act_order 冲突说明已留痕，忽略即可
				log.debug("[OZON][ACT] 订单留痕已存在: actId={}, orderId={}", actId, order.getId());
			}
		}
	}

	// ==================================================================
	// 轮询状态
	// ==================================================================

	/**
	 * 轮询批次状态。<b>单次</b>调用只对每份 PENDING 运单查一次 Ozon 状态；
	 * 就绪则立刻下载 PDF 并存入 OSS，置为 READY。不循环、不阻塞。
	 *
	 * @param batchNo 批次号
	 * @return 批次当前状态（READY 的运单带下载地址）
	 */
	public OzonActBatchVO pollBatch(String batchNo) {
		OzonActBatchVO batch = new OzonActBatchVO();
		batch.setBatchNo(batchNo);
		if (!StringUtils.hasText(batchNo)) {
			return batch;
		}

		for (OzonShipmentAct act : actMapper.selectByBatchNo(batchNo)) {
			if (!act.isTerminal()) {
				refreshAct(act);
			}
			batch.getActs().add(toVO(act, act.getShopId()));
		}
		return batch;
	}

	/** 对单份未就绪运单查一次状态，就绪则取 PDF 落 OSS。 */
	private void refreshAct(OzonShipmentAct act) {
		if (act.getOzonActId() == null) {
			// CREATING 且无 Ozon ID：创建阶段就失败了，不必再查
			return;
		}
		try {
			Shop shop = shopService.getById(act.getShopId());
			if (shop == null) {
				throw new IllegalStateException("店铺不存在: " + act.getShopId());
			}
			OzonCredential credential = credentialService.parseCredential(shop);

			String status = ozonPlatformApi.checkActStatus(credential, act.getOzonActId());
			String normalized = status == null ? "" : status.trim().toLowerCase();

			if (READY_STATUSES.contains(normalized)) {
				byte[] pdf = ozonPlatformApi.getActPdf(credential, act.getOzonActId());
				String fileName = String.format("ozon-act-%d.pdf", act.getOzonActId());
				String objectKey = labelService.uploadPdfToOss(pdf, act.getBatchNo(), fileName);

				act.setFileName(fileName);
				act.setObjectKey(objectKey);
				act.setStatus(OzonShipmentAct.STATUS_READY);
				act.setErrorMsg(null);
				actMapper.updateById(act);
				log.info("[OZON][ACT] 运单就绪: actId={}, ozonActId={}, size={}",
						act.getId(), act.getOzonActId(), pdf.length);
			}
			else if (FAILED_STATUSES.contains(normalized)) {
				act.setStatus(OzonShipmentAct.STATUS_FAILED);
				act.setErrorMsg(truncate("Ozon 返回状态: " + status));
				actMapper.updateById(act);
			}
			else {
				// 未知或 in_process：保持 PENDING，下一轮继续查。
				// 不把未知状态当失败，避免 Ozon 新增状态值时误判。
				if (!OzonShipmentAct.STATUS_PENDING.equals(act.getStatus())) {
					act.setStatus(OzonShipmentAct.STATUS_PENDING);
					actMapper.updateById(act);
				}
			}
		}
		catch (Exception e) {
			// 单次轮询失败不置为 FAILED（可能只是网络抖动），保持 PENDING 等下一轮；
			// 仅记录原因供前端提示。彻底放弃由前端的总超时收口。
			log.warn("[OZON][ACT] 轮询运单状态失败: actId={}, ozonActId={}, error={}",
					act.getId(), act.getOzonActId(), e.getMessage());
		}
	}

	// ==================================================================
	// 校验与工具
	// ==================================================================

	/**
	 * 准备发运的准入校验，与前端 OzonOrderActDialog.canShip() 保持一致。
	 *
	 * @return null 表示可发运；否则返回给货主看的原因
	 */
	private String rejectReason(ErpOrder order) {
		if (!PlatformEnum.Ozon.code().equalsIgnoreCase(order.getPlatform())) {
			return "非 Ozon 平台";
		}
		if (!FulfillmentType.FBS.matches(order.getFulfillmentType())) {
			return "FBO 订单不支持发运";
		}
		if (order.getLocked() != null && order.getLocked() == 1) {
			return "订单已锁定";
		}
		if (!"SHIPPED".equals(order.getErpStatus())) {
			return "状态不支持：" + (order.getErpStatus() == null ? "-" : order.getErpStatus());
		}
		if (!StringUtils.hasText(order.getWarehouseName())) {
			return "缺少仓库信息";
		}
		if (!order.getWarehouseName().contains(BIG_WAREHOUSE_KEYWORD)) {
			return "小仓订单无需运单";
		}
		// 强制前置：必须先打印面单（labelBase64 即面单缓存）
		if (!StringUtils.hasText(order.getLabelBase64())) {
			return "未打印面单";
		}
		if (deliveryMethodIdOf(order) == null) {
			return "缺少物流方式";
		}
		return null;
	}

	/** 从 rawJson 解析物流方式；erp_order 未落该列，沿用 OzonOrderQueryService 的读法。 */
	private OzonDeliveryMethod deliveryMethodOf(ErpOrder order) {
		if (!StringUtils.hasText(order.getRawJson())) {
			return null;
		}
		try {
			OzonPosting posting = objectMapper.readValue(order.getRawJson(), OzonPosting.class);
			return posting.getDeliveryMethod();
		}
		catch (Exception e) {
			log.warn("[OZON][ACT] rawJson 解析失败 orderId={}: {}", order.getId(), e.getMessage());
			return null;
		}
	}

	private Long deliveryMethodIdOf(ErpOrder order) {
		OzonDeliveryMethod dm = deliveryMethodOf(order);
		return dm != null ? dm.getId() : null;
	}

	private OzonActVO toVO(OzonShipmentAct act, Long shopId) {
		OzonActVO vo = new OzonActVO();
		vo.setActId(act.getId());
		vo.setShopId(shopId);
		Shop shop = shopId != null ? shopService.getById(shopId) : null;
		vo.setShopName(shop != null ? shop.getName() : null);
		vo.setDeliveryMethodId(act.getDeliveryMethodId());
		vo.setDeliveryMethodName(act.getDeliveryMethodName());
		vo.setWarehouseName(act.getWarehouseName());
		vo.setDepartureDate(act.getDepartureDate() != null
				? act.getDepartureDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
		vo.setStatus(act.getStatus());
		vo.setOrderCount(act.getOrderCount());
		vo.setFileName(act.getFileName());
		vo.setObjectKey(act.getObjectKey());
		vo.setErrorMsg(act.getErrorMsg());
		if (StringUtils.hasText(act.getObjectKey())) {
			vo.setDownloadUrl(ossService.getDownloadUrl(OssBucketKeys.PUBLIC_FILES, act.getObjectKey()));
		}
		return vo;
	}

	private String generateBatchNo() {
		return "ACT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
	}

	/**
	 * 把平台异常翻译成运营看得懂的原因。
	 */
	private String resolveFailReason(Exception e) {
		String msg = e.getMessage() == null ? "" : e.getMessage();
		String lower = msg.toLowerCase();
		if (lower.contains("aren't ready") || lower.contains("not ready")) {
			return "货件尚未就绪，请稍后重试";
		}
		return "创建运单失败: " + msg;
	}

	private String truncate(String s) {
		if (s == null) {
			return null;
		}
		return s.length() > 500 ? s.substring(0, 500) : s;
	}

	/** 供外部判定仓型（与前端 warehouse-type.ts 同一规则） */
	public static boolean isBigWarehouse(String warehouseName) {
		return StringUtils.hasText(warehouseName) && warehouseName.contains(BIG_WAREHOUSE_KEYWORD);
	}

	/** 过滤出大仓订单（拣货单等其它场景可复用） */
	public static List<ErpOrder> filterBigWarehouse(List<ErpOrder> orders) {
		return orders.stream()
				.filter(Objects::nonNull)
				.filter(o -> isBigWarehouse(o.getWarehouseName()))
				.collect(Collectors.toList());
	}
}
