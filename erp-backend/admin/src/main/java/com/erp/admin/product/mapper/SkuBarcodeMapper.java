package com.erp.admin.product.mapper;

import com.erp.admin.product.model.entity.SkuBarcode;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

public interface SkuBarcodeMapper extends ExtendMapper<SkuBarcode> {

	default List<SkuBarcode> selectBySkuId(Long skuId) {
		return selectList(WrappersX.lambdaQueryX(SkuBarcode.class)
				.eq(SkuBarcode::getSkuId, skuId)
				.eq(SkuBarcode::getEnabled, 1)
				.orderByDesc(SkuBarcode::getPrimaryFlag)
				.orderByAsc(SkuBarcode::getId));
	}

	default List<SkuBarcode> selectBySkuCode(String skuCode) {
		return selectList(WrappersX.lambdaQueryX(SkuBarcode.class)
				.eq(SkuBarcode::getSkuCode, skuCode)
				.eq(SkuBarcode::getEnabled, 1)
				.orderByDesc(SkuBarcode::getPrimaryFlag)
				.orderByAsc(SkuBarcode::getId));
	}

	default SkuBarcode selectByBarcode(String barcode) {
		return selectOne(WrappersX.lambdaQueryX(SkuBarcode.class)
				.eq(SkuBarcode::getBarcode, barcode)
				.eq(SkuBarcode::getEnabled, 1));
	}

	default int deleteBySkuId(Long skuId) {
		return delete(WrappersX.lambdaQueryX(SkuBarcode.class).eq(SkuBarcode::getSkuId, skuId));
	}

}

