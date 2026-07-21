package com.erp.admin.order.service.ozon;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.erp.admin.platform.ozon.OzonClient;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.request.act.OzonActCreateRequest;
import com.erp.admin.platform.ozon.model.request.label.OzonPackageLabelRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonPackage;
import com.erp.admin.platform.ozon.model.request.posting.OzonPackageProduct;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingGetRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonShipRequest;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OzonPlatformApi - 面向 Ozon 平台的纯平台交互层
 * <p>
 * 职责：
 * - 只负责与 Ozon API 交互，不涉及数据库操作
 * - 发货订单（仅 FBS 订单）
 * - 批量获取订单状态
 * - 获取面单（仅 FBS 订单）
 * <p>
 * 注意：不包含数据库操作，由调用方（OzonOrderErpService）负责
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonPlatformApi {

	private final OzonClient ozonClient;

	/**
	 * 发货订单（打包订单）
	 * <p>
	 * API: POST /v4/posting/fbs/ship
	 * <p>
	 * 将订单状态更改为 awaiting_deliver（等待发货）
	 * <p>
	 * 注意：
	 * - 只支持 FBS 订单
	 * - FBO 订单由 Ozon 仓库发货，不能调用此方法
	 * - 此方法只负责调用 API，不更新数据库
	 *
	 * @param credential    Ozon 凭证
	 * @param postingNumber 发货单号
	 * @param packages      包裹列表（包含商品信息）
	 */
	public void shipPosting(OzonCredential credential, String postingNumber, List<OzonPackage> packages) {
		log.info("[OZON] 开始发货订单: postingNumber={}", postingNumber);

		OzonShipRequest request = OzonShipRequest.builder()
				.postingNumber(postingNumber)
				.packages(packages)
				.build();

		ozonClient.shipPosting(credential, request);

		log.info("[OZON] 订单发货成功: postingNumber={}", postingNumber);
	}

	/**
	 * 批量获取订单状态
	 * <p>
	 * 通过订单详情 API 批量获取订单的最新状态
	 * <p>
	 * 注意：此方法只负责调用 API，不更新数据库
	 *
	 * @param credential     Ozon 凭证
	 * @param postingNumbers 发货单号列表
	 * @return 发货单号到订单详情的映射
	 */
	public Map<String, OzonPosting> batchFetchPostings(OzonCredential credential, List<String> postingNumbers) {
		Map<String, OzonPosting> postingMap = new HashMap<>();

		if (postingNumbers == null || postingNumbers.isEmpty()) {
			return postingMap;
		}

		log.info("[OZON] 开始批量获取订单状态: count={}", postingNumbers.size());

		// 逐个获取订单详情（Ozon API 不支持批量查询订单详情）
		for (String postingNumber : postingNumbers) {
			try {
				OzonPostingGetRequest request = OzonPostingGetRequest.builder()
						.postingNumber(postingNumber)
						.build();

				OzonPosting posting = ozonClient.getPosting(credential, request);
				if (posting != null) {
					postingMap.put(postingNumber, posting);
				}
			} catch (Exception e) {
				log.warn("[OZON] 获取订单详情失败: postingNumber={}, error={}", postingNumber, e.getMessage());
			}
		}

		log.info("[OZON] 批量获取订单状态完成: 成功={}/{}", postingMap.size(), postingNumbers.size());

		return postingMap;
	}

	/**
	 * 获取面单（PDF 格式）
	 * <p>
	 * API: POST /v2/posting/fbs/package-label
	 * <p>
	 * 注意：
	 * - 只支持 FBS 订单
	 * - FBO 订单由 Ozon 仓库发货，不能获取面单
	 * - 最多支持 20 个发货单号
	 * - 此方法只负责调用 API，不更新数据库
	 * - 返回解码后的 PDF 字节数组
	 *
	 * @param credential     Ozon 凭证
	 * @param postingNumber 发货单号
	 * @return PDF 字节数组
	 */
	public byte[] getPackageLabel(OzonCredential credential, String postingNumber) {
		// 为了后续复用面单，这里强制只支持单个发货单号
		if (postingNumber == null || postingNumber.isEmpty()) {
			throw new IllegalArgumentException("发货单号列表不能为空");
		}

		log.info("[OZON] 开始获取面单");

		OzonPackageLabelRequest request = OzonPackageLabelRequest.builder()
				.postingNumber(Collections.singletonList(postingNumber))
				.build();

		byte[] fileContent = ozonClient.getPackageLabel(credential, request);
		
		if (fileContent == null || fileContent.length == 0) {
			throw new IllegalStateException("获取面单响应为空");
		}

		log.info("[OZON] 获取面单成功: size={} bytes", fileContent.length);

		return fileContent;
	}

	// ========================= 运单（交接单 act） =========================

	/**
	 * 创建运单。
	 * <p>
	 * 运单按「物流方式 + 发货日期」维度生成，内容由 Ozon 汇总当日该物流方式下的全部待发货件。
	 * 该接口立即返回运单 ID，PDF 需等 Ozon 异步生成完毕（见 {@link #checkActStatus}）。
	 *
	 * @param credential       Ozon 凭证
	 * @param deliveryMethodId 物流方式 ID
	 * @param departureDate    发货日期（yyyy-MM-dd）
	 * @param containersCount  箱数
	 * @return Ozon 侧运单 ID
	 */
	public Long createAct(OzonCredential credential, Long deliveryMethodId, LocalDate departureDate,
			int containersCount) {
		if (deliveryMethodId == null) {
			throw new IllegalArgumentException("物流方式ID不能为空");
		}
		if (departureDate == null) {
			throw new IllegalArgumentException("发货日期不能为空");
		}

		OzonActCreateRequest request = OzonActCreateRequest.builder()
				.deliveryMethodId(deliveryMethodId)
				.departureDate(departureDate.atStartOfDay().atOffset(ZoneOffset.UTC)
						.format(DateTimeFormatter.ISO_INSTANT))
				.containersCount(containersCount)
				.build();

		return ozonClient.createAct(credential, request);
	}

	/**
	 * 查询运单生成状态，返回 Ozon 原始状态串（如 in_process / ready）。
	 * <p>
	 * 状态取值未在官方 OpenAPI 中收录，调用方不应把未知状态当作失败。
	 *
	 * @param credential Ozon 凭证
	 * @param actId      Ozon 侧运单 ID
	 * @return 原始状态串
	 */
	public String checkActStatus(OzonCredential credential, Long actId) {
		if (actId == null) {
			throw new IllegalArgumentException("运单ID不能为空");
		}
		return ozonClient.checkActStatus(credential, actId);
	}

	/**
	 * 下载运单 PDF。必须在状态就绪后调用。
	 *
	 * @param credential Ozon 凭证
	 * @param actId      Ozon 侧运单 ID
	 * @return PDF 字节数组
	 */
	public byte[] getActPdf(OzonCredential credential, Long actId) {
		if (actId == null) {
			throw new IllegalArgumentException("运单ID不能为空");
		}

		byte[] fileContent = ozonClient.getActPdf(credential, actId);
		if (fileContent == null || fileContent.length == 0) {
			throw new IllegalStateException("获取运单响应为空");
		}
		return fileContent;
	}

	/**
	 * 构建发货包裹信息
	 * <p>
	 * 根据订单的商品列表构建发货包裹
	 *
	 * @param products 商品列表
	 * @return 包裹列表
	 */
	public List<OzonPackage> buildPackages(List<OzonProduct> products) {
		if (products == null || products.isEmpty()) {
			throw new IllegalArgumentException("商品列表不能为空");
		}

		List<OzonPackageProduct> packageProducts = products.stream()
				.map(product -> OzonPackageProduct.builder()
						.productId(product.getSku())
						.quantity(product.getQuantity())
						.build())
				.collect(Collectors.toList());

		OzonPackage ozonPackage = OzonPackage.builder()
				.products(packageProducts)
				.build();

		List<OzonPackage> packages = new ArrayList<>();
		packages.add(ozonPackage);

		return packages;
	}
}
