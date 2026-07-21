package com.erp.admin.wms.service.handler;

import com.erp.admin.wms.mapper.PurchaseOrderFileMapper;
import com.erp.admin.wms.model.dto.PaymentInfoDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.entity.PurchaseOrderFile;
import com.erp.admin.wms.model.enums.PurchaseOrderFileType;
import com.erp.admin.wms.model.enums.PurchaseOrderStatus;
import com.erp.admin.wms.service.validator.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 * 付款处理器
 * <p>
 * 处理付款状态变更和凭证关联
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHandler {

	private final PurchaseOrderFileMapper purchaseOrderFileMapper;

	private final FileTypeValidator fileTypeValidator;

	/**
	 * 处理付款信息变更
	 *
	 * @param order       采购单实体
	 * @param paymentInfo 付款信息DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void handlePaymentChange(PurchaseOrder order, PaymentInfoDTO paymentInfo) {
		if (paymentInfo == null) {
			return;
		}

		// 校验采购单状态是否允许修改付款信息
		validateOrderStatus(order);

		// 处理首付款状态变更
		if (paymentInfo.getPrepayStatus() != null) {
			handlePrepayChange(order, paymentInfo);
		}

		// 处理尾款状态变更
		if (paymentInfo.getBalanceStatus() != null) {
			handleBalanceChange(order, paymentInfo);
		}
	}

	/**
	 * 处理首付款状态变更
	 */
	private void handlePrepayChange(PurchaseOrder order, PaymentInfoDTO paymentInfo) {
		boolean newStatus = Integer.valueOf(1).equals(paymentInfo.getPrepayStatus());
		boolean oldStatus = Integer.valueOf(1).equals(order.getPrepayStatus());

		if (newStatus && !oldStatus) {
			// 未付 -> 已付：必须有凭证
			Assert.notNull(paymentInfo.getPrepayVoucherFileId(), "首付款已付时必须上传凭证");
			fileTypeValidator.validateVoucherFile(paymentInfo.getPrepayVoucherFileId());

			order.setPrepayStatus(1);
			order.setPrepayTime(LocalDateTime.now());

			// 保存凭证关联
			saveVoucherFile(order.getId(), paymentInfo.getPrepayVoucherFileId(),
					PurchaseOrderFileType.PREPAY_VOUCHER);

			log.info("Prepay status changed to paid, orderId={}, voucherFileId={}",
					order.getId(), paymentInfo.getPrepayVoucherFileId());

		} else if (!newStatus && oldStatus) {
			// 已付 -> 未付：清空凭证和时间
			order.setPrepayStatus(0);
			order.setPrepayTime(null);

			// 删除凭证关联
			deleteVoucherFile(order.getId(), PurchaseOrderFileType.PREPAY_VOUCHER);

			log.info("Prepay status changed to unpaid, orderId={}", order.getId());

		} else if (newStatus) {
			// 保持已付状态，但可能更换凭证
			if (paymentInfo.getPrepayVoucherFileId() != null) {
				// 检查是否需要更换凭证
				PurchaseOrderFile existingVoucher = purchaseOrderFileMapper.selectByOrderIdAndType(
						order.getId(), PurchaseOrderFileType.PREPAY_VOUCHER.name());

				if (existingVoucher == null ||
						!existingVoucher.getSysFileId().equals(paymentInfo.getPrepayVoucherFileId())) {
					fileTypeValidator.validateVoucherFile(paymentInfo.getPrepayVoucherFileId());

					// 删除旧凭证
					if (existingVoucher != null) {
						purchaseOrderFileMapper.deleteById(existingVoucher.getId());
					}

					// 保存新凭证
					saveVoucherFile(order.getId(), paymentInfo.getPrepayVoucherFileId(),
							PurchaseOrderFileType.PREPAY_VOUCHER);

					log.info("Prepay voucher replaced, orderId={}, newVoucherFileId={}",
							order.getId(), paymentInfo.getPrepayVoucherFileId());
				}
			}
		}
	}

	/**
	 * 处理尾款状态变更
	 */
	private void handleBalanceChange(PurchaseOrder order, PaymentInfoDTO paymentInfo) {
		boolean newStatus = Integer.valueOf(1).equals(paymentInfo.getBalanceStatus());
		boolean oldStatus = Integer.valueOf(1).equals(order.getBalanceStatus());

		if (newStatus && !oldStatus) {
			// 未付 -> 已付：必须有凭证
			Assert.notNull(paymentInfo.getBalanceVoucherFileId(), "尾款已付时必须上传凭证");
			fileTypeValidator.validateVoucherFile(paymentInfo.getBalanceVoucherFileId());

			order.setBalanceStatus(1);
			order.setBalancePayTime(LocalDateTime.now());

			// 保存凭证关联
			saveVoucherFile(order.getId(), paymentInfo.getBalanceVoucherFileId(),
					PurchaseOrderFileType.BALANCE_VOUCHER);

			log.info("Balance status changed to paid, orderId={}, voucherFileId={}",
					order.getId(), paymentInfo.getBalanceVoucherFileId());

		} else if (!newStatus && oldStatus) {
			// 已付 -> 未付：清空凭证和时间
			order.setBalanceStatus(0);
			order.setBalancePayTime(null);

			// 删除凭证关联
			deleteVoucherFile(order.getId(), PurchaseOrderFileType.BALANCE_VOUCHER);

			log.info("Balance status changed to unpaid, orderId={}", order.getId());

		} else if (newStatus) {
			// 保持已付状态，但可能更换凭证
			if (paymentInfo.getBalanceVoucherFileId() != null) {
				// 检查是否需要更换凭证
				PurchaseOrderFile existingVoucher = purchaseOrderFileMapper.selectByOrderIdAndType(
						order.getId(), PurchaseOrderFileType.BALANCE_VOUCHER.name());

				if (existingVoucher == null ||
						!existingVoucher.getSysFileId().equals(paymentInfo.getBalanceVoucherFileId())) {
					fileTypeValidator.validateVoucherFile(paymentInfo.getBalanceVoucherFileId());

					// 删除旧凭证
					if (existingVoucher != null) {
						purchaseOrderFileMapper.deleteById(existingVoucher.getId());
					}

					// 保存新凭证
					saveVoucherFile(order.getId(), paymentInfo.getBalanceVoucherFileId(),
							PurchaseOrderFileType.BALANCE_VOUCHER);

					log.info("Balance voucher replaced, orderId={}, newVoucherFileId={}",
							order.getId(), paymentInfo.getBalanceVoucherFileId());
				}
			}
		}
	}

	/**
	 * 保存凭证文件关联
	 */
	private void saveVoucherFile(Long orderId, Long sysFileId, PurchaseOrderFileType fileType) {
		PurchaseOrderFile file = new PurchaseOrderFile();
		file.setPurchaseOrderId(orderId);
		file.setSysFileId(sysFileId);
		file.setFileType(fileType.name());
		file.setCreateTime(LocalDateTime.now());
		purchaseOrderFileMapper.insert(file);
	}

	/**
	 * 删除凭证文件关联
	 */
	private void deleteVoucherFile(Long orderId, PurchaseOrderFileType fileType) {
		PurchaseOrderFile existingFile = purchaseOrderFileMapper.selectByOrderIdAndType(
				orderId, fileType.name());
		if (existingFile != null) {
			purchaseOrderFileMapper.deleteById(existingFile.getId());
		}
	}

	/**
	 * 校验采购单状态是否允许修改付款信息
	 */
	private void validateOrderStatus(PurchaseOrder order) {
		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());
		Assert.isTrue(status != PurchaseOrderStatus.COMPLETED && status != PurchaseOrderStatus.CANCELLED,
				"已完成或已取消的采购单不允许修改付款信息");
	}

}
