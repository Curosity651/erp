package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.model.entity.TransferOrder;
import com.erp.admin.wms.model.qo.TransferOrderQO;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.springframework.util.StringUtils;

/**
 * 调拨单 Mapper
 *
 * @author erp
 */
public interface TransferOrderMapper extends ExtendMapper<TransferOrder> {

	/**
	 * 分页查询（default 方法，使用 Wrapper）
	 */
	default IPage<TransferOrder> queryPage(IPage<TransferOrder> page, TransferOrderQO qo) {
		return selectPage(page, Wrappers.<TransferOrder>lambdaQuery()
			.like(StringUtils.hasText(qo.getTransferNo()), TransferOrder::getTransferNo, qo.getTransferNo())
			.eq(StringUtils.hasText(qo.getTransferType()), TransferOrder::getTransferType, qo.getTransferType())
			.eq(qo.getFromWarehouseId() != null, TransferOrder::getFromWarehouseId, qo.getFromWarehouseId())
			.eq(qo.getToWarehouseId() != null, TransferOrder::getToWarehouseId, qo.getToWarehouseId())
			.eq(StringUtils.hasText(qo.getOrderStatus()), TransferOrder::getOrderStatus, qo.getOrderStatus())
			.ge(qo.getCreateTimeStart() != null, TransferOrder::getCreateTime, qo.getCreateTimeStart())
			.le(qo.getCreateTimeEnd() != null, TransferOrder::getCreateTime, qo.getCreateTimeEnd())
			.orderByDesc(TransferOrder::getId)
		);
	}

}
