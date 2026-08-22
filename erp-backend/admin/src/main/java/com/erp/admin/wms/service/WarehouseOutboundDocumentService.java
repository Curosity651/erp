package com.erp.admin.wms.service;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.model.vo.OzonActBatchVO;
import com.erp.admin.order.service.label.LabelPrintOrchestrator;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.order.service.ozon.OzonActService;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.SalesOutboundMapper;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.enums.OutboundSourceType;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseOutboundDocumentService {

	private final SalesOutboundMapper salesOutboundMapper;
	private final SalesOutboundPackageService packageService;
	private final ErpOrderMapper erpOrderMapper;
	private final LabelPrintOrchestrator labelPrintOrchestrator;
	private final OzonActService ozonActService;
	private final TenantIdentityService tenantIdentityService;
	private final LabelService labelService;

	public LabelBatchVO prepareLabels(Long outboundOrderId, Long userId) {
		SalesOutboundOrder order = getOperableSalesOrder(outboundOrderId);
		Assert.isTrue("WAREHOUSE_PRINT".equals(order.getDocumentMode()), "该出库单使用货主提供的资料");
		Assert.isTrue(OutboundOrderStatus.PICKED.name().equals(order.getOrderStatus()),
				"请先完成拣货和订单级分货");
		List<Long> orderIds = orderIds(outboundOrderId);
		LabelBatchVO result = TenantContext.runAs(order.getErpTenantId(),
				() -> labelPrintOrchestrator.printLabels(order.getPlatform(), orderIds, userId,
						"海外仓出库单 " + order.getOutboundNo()));
		List<Long> readyIds = TenantContext.runAs(order.getErpTenantId(), () -> erpOrderMapper.selectByIds(orderIds)
				.stream().filter(item -> StringUtils.hasText(item.getLabelBase64()))
				.map(ErpOrder::getId).collect(Collectors.toList()));
		packageService.markLabelReady(outboundOrderId, readyIds);
		return result;
	}

	public LabelBatchVO latestLabels(Long outboundOrderId) {
		SalesOutboundOrder order = getOperableSalesOrder(outboundOrderId);
		List<Long> orderIds = orderIds(outboundOrderId);
		return TenantContext.runAs(order.getErpTenantId(),
				() -> labelService.getLatestBatchVO(orderIds));
	}

	public OzonActBatchVO prepareOzonActs(Long outboundOrderId, LocalDate departureDate, Long userId) {
		SalesOutboundOrder order = getOperableSalesOrder(outboundOrderId);
		Assert.isTrue("WAREHOUSE_PRINT".equals(order.getDocumentMode()), "货主提供资料模式无需仓库生成交接单");
		Assert.isTrue("ozon".equalsIgnoreCase(order.getPlatform()), "仅 Ozon 出库单支持交接单");
		List<Long> orderIds = orderIds(outboundOrderId);
		OzonActBatchVO result = TenantContext.runAs(order.getErpTenantId(), () -> {
			List<Long> requiredIds = erpOrderMapper.selectByIds(orderIds).stream()
					.filter(ozonActService::isActRequired)
					.map(ErpOrder::getId).collect(Collectors.toList());
			return ozonActService.createActs(requiredIds, departureDate, userId);
		});
		refreshHandoverReady(order);
		return result;
	}

	public OzonActBatchVO pollOzonActs(Long outboundOrderId, String batchNo) {
		SalesOutboundOrder order = getOperableSalesOrder(outboundOrderId);
		OzonActBatchVO result = TenantContext.runAs(order.getErpTenantId(), () -> ozonActService.pollBatch(batchNo));
		refreshHandoverReady(order);
		return result;
	}

	/** 最终签出前的服务端资料准入。 */
	public void assertReadyForShip(SalesOutboundOrder order) {
		if (!OutboundSourceType.SALES.name().equals(order.getSourceType())
				|| !"ozon".equalsIgnoreCase(order.getPlatform())) {
			return;
		}
		List<Long> orderIds = orderIds(order.getId());
		java.util.Map<Long, WmsSalesOutboundPackage> packageByOrder = packageService
				.listEntities(order.getId()).stream().collect(Collectors.toMap(
						WmsSalesOutboundPackage::getErpOrderId, value -> value, (a, b) -> a));
		TenantContext.runAs(order.getErpTenantId(), () -> {
			for (ErpOrder erpOrder : erpOrderMapper.selectByIds(orderIds)) {
				if (!ozonActService.isActRequired(erpOrder)) {
					continue;
				}
				if ("OWNER_PROVIDED".equals(order.getDocumentMode())) {
					WmsSalesOutboundPackage pack = packageByOrder.get(erpOrder.getId());
					if (pack == null || !SalesOutboundPackageService.HANDOVER_EXTERNAL_CONFIRMED
							.equals(pack.getHandoverStatus())) {
						throw new BusinessException(400,
								"请先核对货主提供的Ozon交接单，订单: " + erpOrder.getPlatformOrderId());
					}
				}
				else if (!ozonActService.hasReadyAct(erpOrder.getId())) {
					throw new BusinessException(400,
							"Ozon 交接单尚未就绪，订单: " + erpOrder.getPlatformOrderId());
				}
			}
			return null;
		});
	}

	/**
	 * Package-level sign-out gate. One unfinished package must not block another
	 * package that has its own required handover document ready.
	 */
	public void assertPackageReadyForShip(SalesOutboundOrder order, WmsSalesOutboundPackage pack) {
		if (!OutboundSourceType.SALES.name().equals(order.getSourceType())
				|| !"ozon".equalsIgnoreCase(order.getPlatform())
				|| !Integer.valueOf(1).equals(pack.getHandoverRequired())) {
			return;
		}
		if ("OWNER_PROVIDED".equals(order.getDocumentMode())) {
			Assert.isTrue(SalesOutboundPackageService.HANDOVER_EXTERNAL_CONFIRMED
							.equals(pack.getHandoverStatus()),
					"请先核对货主提供的Ozon交接单，订单: " + pack.getPlatformOrderId());
			return;
		}
		boolean ready = SalesOutboundPackageService.HANDOVER_READY.equals(pack.getHandoverStatus())
				|| TenantContext.runAs(order.getErpTenantId(),
						() -> ozonActService.hasReadyAct(pack.getErpOrderId()));
		Assert.isTrue(ready, "Ozon交接单尚未就绪，订单: " + pack.getPlatformOrderId());
	}

	private void refreshHandoverReady(SalesOutboundOrder order) {
		List<Long> readyIds = TenantContext.runAs(order.getErpTenantId(), () -> orderIds(order.getId()).stream()
				.filter(ozonActService::hasReadyAct).collect(Collectors.toList()));
		packageService.markHandoverReady(readyIds);
	}

	private SalesOutboundOrder getOperableSalesOrder(Long id) {
		assertPlatform();
		SalesOutboundOrder order = salesOutboundMapper.selectById(id);
		Assert.notNull(order, "出库单不存在");
		Assert.isTrue(OutboundSourceType.SALES.name().equals(order.getSourceType()), "仅销售出库单支持平台资料");
		return order;
	}

	private List<Long> orderIds(Long outboundOrderId) {
		List<Long> ids = packageService.listEntities(outboundOrderId).stream()
				.map(WmsSalesOutboundPackage::getErpOrderId).distinct().collect(Collectors.toList());
		Assert.notEmpty(ids, "出库单未关联平台订单");
		return ids;
	}

	private void assertPlatform() {
		String identityType = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identityType)) {
			throw new BusinessException(403, "仅海外仓平台可处理出库资料");
		}
	}
}
