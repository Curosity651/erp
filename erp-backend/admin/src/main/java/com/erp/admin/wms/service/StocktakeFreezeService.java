package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.StocktakeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class StocktakeFreezeService {

	private final StocktakeMapper stocktakeMapper;

	public void assertLocationMutable(Long warehouseId, String locationCode) {
		if (warehouseId == null || locationCode == null || locationCode.isEmpty()) {
			return;
		}
		Assert.isTrue(stocktakeMapper.countActiveLocationFreeze(warehouseId, locationCode) == 0,
				"库位正在盘点，暂时不能进行库存作业：" + locationCode);
	}

}
