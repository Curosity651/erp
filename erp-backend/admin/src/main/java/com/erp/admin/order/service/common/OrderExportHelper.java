package com.erp.admin.order.service.common;

import java.util.Collections;
import java.util.List;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.qo.ErpOrderQO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 订单导出通用 Helper
 * <p>
 * 封装分批查询通用逻辑。
 */
@Component
@RequiredArgsConstructor
public class OrderExportHelper {

	private final ErpOrderMapper erpOrderMapper;

	/**
	 * 分批查询导出数据
	 *
	 * @param qo          已预处理的查询条件
	 * @param currentPage 当前页码（从 1 开始）
	 * @param pageSize    每页大小
	 * @return 订单实体列表
	 */
	public List<ErpOrder> queryExportBatch(ErpOrderQO qo, int currentPage, int pageSize) {
		int offset = (currentPage - 1) * pageSize;
		List<ErpOrder> orders = erpOrderMapper.selectExportBatch(qo, offset, pageSize);
		return CollectionUtils.isEmpty(orders) ? Collections.emptyList() : orders;
	}

}
