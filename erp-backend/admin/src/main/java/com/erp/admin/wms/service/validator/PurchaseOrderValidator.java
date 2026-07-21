package com.erp.admin.wms.service.validator;

import com.erp.admin.wms.model.dto.ContractInfoDTO;
import com.erp.admin.wms.model.dto.PaymentInfoDTO;
import com.erp.admin.wms.model.dto.PurchaseOrderDTO;
import com.erp.admin.wms.model.dto.QcDataDTO;
import com.erp.admin.wms.model.dto.QcItemDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.enums.PurchaseOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 采购单业务校验器
 * <p>
 * 用于校验采购单相关的业务规则
 *
 * @author erp
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

	private final FileTypeValidator fileTypeValidator;

	/**
	 * 校验付款信息
	 * <p>
	 * 规则：付款状态为已付时必须有对应凭证
	 *
	 * @param paymentInfo 付款信息DTO
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validatePaymentInfo(PaymentInfoDTO paymentInfo) {
		if (paymentInfo == null) {
			return;
		}

		// 首付款已付时必须有凭证
		if (Integer.valueOf(1).equals(paymentInfo.getPrepayStatus())) {
			Assert.notNull(paymentInfo.getPrepayVoucherFileId(),
					"首付款状态为已付时必须上传凭证");
			fileTypeValidator.validateVoucherFile(paymentInfo.getPrepayVoucherFileId());
		}

		// 尾款已付时必须有凭证
		if (Integer.valueOf(1).equals(paymentInfo.getBalanceStatus())) {
			Assert.notNull(paymentInfo.getBalanceVoucherFileId(),
					"尾款状态为已付时必须上传凭证");
			fileTypeValidator.validateVoucherFile(paymentInfo.getBalanceVoucherFileId());
		}
	}

	/**
	 * 校验质检数据
	 * <p>
	 * 规则：毛重必须大于等于净重，质检报告文件类型必须正确
	 *
	 * @param qcData 质检数据DTO
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validateQcData(QcDataDTO qcData) {
		if (qcData == null || qcData.getItems() == null) {
			return;
		}

		for (QcItemDTO item : qcData.getItems()) {
			// 校验毛重 >= 净重
			if (item.getGrossWeightKg() != null && item.getNetWeightKg() != null) {
				Assert.isTrue(item.getGrossWeightKg().compareTo(item.getNetWeightKg()) >= 0,
						String.format("SKU[%s]的毛重必须大于等于净重", item.getSkuCode()));
			}

			// 校验质检报告文件类型
			if (item.getQcFileId() != null) {
				fileTypeValidator.validateQcReportFile(item.getQcFileId());
			}
		}
	}

	/**
	 * 校验合同信息
	 * <p>
	 * 规则：合同文件类型必须正确
	 *
	 * @param contractInfo 合同信息DTO
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validateContractInfo(ContractInfoDTO contractInfo) {
		if (contractInfo == null || contractInfo.getContractFileId() == null) {
			return;
		}

		// 有操作时校验文件类型
		if (contractInfo.getAction() != null) {
			fileTypeValidator.validateContractFile(contractInfo.getContractFileId());
		}
	}

	/**
	 * 校验字段可编辑性
	 * <p>
	 * 根据采购单状态校验是否允许编辑，以及是否修改了不可编辑的字段
	 *
	 * @param order 采购单实体
	 * @param dto   采购单DTO
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validateFieldEditable(PurchaseOrder order, PurchaseOrderDTO dto) {
		PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(order.getOrderStatus());

		// 校验是否可以进入编辑页
		Assert.isTrue(canEnterEditPage(status), "当前状态不允许编辑");

		// 非草稿状态下，校验是否修改了不可编辑的字段
		if (status != PurchaseOrderStatus.DRAFT) {
			// 供应商不可修改
			Assert.isTrue(Objects.equals(order.getSupplierId(), dto.getSupplierId()),
					"当前状态下供应商不可修改");

			// 下单日期不可修改
			Assert.isTrue(Objects.equals(order.getOrderDate(), dto.getOrderDate()),
					"当前状态下下单日期不可修改");

			// 预计交货日期不可修改
			Assert.isTrue(Objects.equals(order.getExpectedDeliveryDate(), dto.getExpectedDeliveryDate()),
					"当前状态下预计交货日期不可修改");

			// 币种不可修改
			Assert.isTrue(Objects.equals(order.getCurrencyCode(), dto.getCurrencyCode()),
					"当前状态下币种不可修改");

			// 是否含税不可修改
			Assert.isTrue(Objects.equals(order.getTaxIncluded(), dto.getTaxIncluded()),
					"当前状态下是否含税不可修改");

			// 首付款比例不可修改
			if (order.getPrepayRatio() != null && dto.getPrepayRatio() != null) {
				Assert.isTrue(order.getPrepayRatio().compareTo(dto.getPrepayRatio()) == 0,
						"当前状态下首付款比例不可修改");
			} else {
				Assert.isTrue(Objects.equals(order.getPrepayRatio(), dto.getPrepayRatio()),
						"当前状态下首付款比例不可修改");
			}

			// 尾款账期不可修改
			Assert.isTrue(Objects.equals(order.getBalancePaymentDays(), dto.getBalancePaymentDays()),
					"当前状态下尾款账期不可修改");

			// 采购明细不可修改（通过items是否为空或null来判断是否有修改意图）
			// 注意：实际的明细比对逻辑较复杂，这里只做基本校验
			// 如果前端传了items，需要在Service层进一步校验是否有实际变更
		}
	}

	/**
	 * 判断是否可以进入编辑页
	 *
	 * @param status 采购单状态
	 * @return 是否可以进入编辑页
	 */
	public boolean canEnterEditPage(PurchaseOrderStatus status) {
		return status != PurchaseOrderStatus.CANCELLED;
	}

}
