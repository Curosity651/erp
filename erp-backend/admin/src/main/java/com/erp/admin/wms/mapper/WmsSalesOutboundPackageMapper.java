package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.Collections;
import java.util.List;

public interface WmsSalesOutboundPackageMapper extends ExtendMapper<WmsSalesOutboundPackage> {

	@Select("SELECT * FROM wms_sales_outbound_package WHERE id = #{id} FOR UPDATE")
	WmsSalesOutboundPackage selectByIdForUpdate(@Param("id") Long id);

	default List<WmsSalesOutboundPackage> selectByOutboundOrderId(Long outboundOrderId) {
		if (outboundOrderId == null) {
			return Collections.emptyList();
		}
		return selectList(Wrappers.lambdaQuery(WmsSalesOutboundPackage.class)
				.eq(WmsSalesOutboundPackage::getOutboundOrderId, outboundOrderId)
				.orderByAsc(WmsSalesOutboundPackage::getId));
	}

	default int deleteByOutboundOrderId(Long outboundOrderId) {
		return delete(Wrappers.lambdaQuery(WmsSalesOutboundPackage.class)
				.eq(WmsSalesOutboundPackage::getOutboundOrderId, outboundOrderId));
	}

}
