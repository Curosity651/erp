package com.erp.admin.wms.service.helper;

import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.wms.model.entity.ShippingOrder;
import com.erp.admin.wms.model.enums.PaymentStatus;
import com.erp.admin.wms.model.enums.ShippingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 物流单付款状态处理辅助类
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingOrderPaymentHelper {

	private final SysFileService sysFileService;

	/**
	 * 处理付款信息（校验 + 设置字段）
	 *
	 * @param order 物流单实体
	 * @param newPaymentStatus 新付款状态（null 默认为未付款）
	 * @param voucherFileId 付款凭证文件ID
	 */
	public void processPaymentInfo(ShippingOrder order, Integer newPaymentStatus, Long voucherFileId) {
		if (newPaymentStatus == null) {
			newPaymentStatus = PaymentStatus.UNPAID.getValue();
		}

		// 校验付款状态不可逆（仅已有订单需要校验）
		if (order.getId() != null && PaymentStatus.isPaid(order.getPaymentStatus())) {
			Assert.isTrue(PaymentStatus.PAID.getValue().equals(newPaymentStatus),
				"已付款的物流单不能改回未付款状态");
		}

		// 已付款状态必须有凭证
		if (PaymentStatus.PAID.getValue().equals(newPaymentStatus)) {
			Assert.notNull(voucherFileId, "付款状态为已付时必须上传凭证");
			SysFileVO fileVO = sysFileService.getFileInfo(voucherFileId);
			Assert.notNull(fileVO, "付款凭证文件不存在");
			order.setPaymentVoucherFileId(voucherFileId);
		}

		order.setPaymentStatus(newPaymentStatus);
	}

	/**
	 * 检查并更新完成状态
	 * 条件：当前为全部到货 + 已付款 → 变更为已完成
	 *
	 * @param order 物流单实体
	 * @return 是否变更为已完成
	 */
	public boolean checkAndUpdateCompletedStatus(ShippingOrder order) {
		ShippingStatus currentStatus = ShippingStatus.valueOf(order.getShippingStatus());

		// 只有全部到货状态且已付款才能变为已完成
		if (currentStatus == ShippingStatus.ALL_ARRIVED && PaymentStatus.isPaid(order.getPaymentStatus())) {
			order.setShippingStatus(ShippingStatus.COMPLETED.name());
			log.info("Shipping order completed, id={}, shippingNo={}", order.getId(), order.getShippingNo());
			return true;
		}
		return false;
	}

}
