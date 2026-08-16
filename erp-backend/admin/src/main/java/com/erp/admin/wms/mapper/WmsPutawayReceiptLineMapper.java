package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsPutawayReceiptLine;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.util.List;

@Mapper
public interface WmsPutawayReceiptLineMapper extends ExtendMapper<WmsPutawayReceiptLine> {

	@Select("SELECT pallet_id, pallet_no, slot_code, location_id, "
			+ "COALESCE(location_code, slot_code) AS location_code, sku_code, quality, quantity, override_reason "
			+ "FROM wms_putaway_receipt_line WHERE inbound_order_id = #{inboundOrderId} "
			+ "ORDER BY COALESCE(location_code, slot_code), sku_code")
	List<PutawayReceiptLineVO> selectByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);

}
