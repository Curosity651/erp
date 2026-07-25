package com.erp.admin.order.mapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.erp.admin.order.model.entity.OzonShipmentAct;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Ozon 运单 Mapper。
 * <p>
 * 所有查询均由多租户拦截器自动追加 tenant_id 条件，无需手工传租户。
 *
 * @author system
 */
public interface OzonShipmentActMapper extends ExtendMapper<OzonShipmentAct> {

	default OzonShipmentAct selectByRequestKey(String requestKey) {
		return selectOne(WrappersX.<OzonShipmentAct>lambdaQueryX()
				.eq(OzonShipmentAct::getRequestKey, requestKey));
	}

	@Update("UPDATE ozon_shipment_act SET status='CREATING', batch_no=#{batchNo}, error_msg=NULL, " +
			"order_count=#{orderCount}, created_by=#{userId}, ozon_act_id=NULL, object_key=NULL, " +
			"file_name=NULL, update_time=NOW() WHERE request_key=#{requestKey} AND status='FAILED'")
	int retryFailed(@Param("requestKey") String requestKey, @Param("batchNo") String batchNo,
			@Param("orderCount") Integer orderCount, @Param("userId") Long userId);

	/** 按批次号取该批全部运单（前端轮询用） */
	default List<OzonShipmentAct> selectByBatchNo(String batchNo) {
		LambdaQueryWrapperX<OzonShipmentAct> wrapper = WrappersX.lambdaQueryX(OzonShipmentAct.class)
				.eq(OzonShipmentAct::getBatchNo, batchNo)
				.orderByAsc(OzonShipmentAct::getId);
		return this.selectList(wrapper);
	}

	/**
	 * 幂等查找：同一 (店铺, 物流方式, 发货日期) 是否已有未失败的运单。
	 * <p>
	 * 命中则复用，避免在 Ozon 侧重复创建运单。
	 */
	default OzonShipmentAct selectReusable(Long shopId, Long deliveryMethodId, LocalDate departureDate) {
		LambdaQueryWrapperX<OzonShipmentAct> wrapper = WrappersX.lambdaQueryX(OzonShipmentAct.class)
				.eq(OzonShipmentAct::getShopId, shopId)
				.eq(OzonShipmentAct::getDeliveryMethodId, deliveryMethodId)
				.eq(OzonShipmentAct::getDepartureDate, departureDate)
				.in(OzonShipmentAct::getStatus,
						Arrays.asList(OzonShipmentAct.STATUS_CREATING,
								OzonShipmentAct.STATUS_PENDING,
								OzonShipmentAct.STATUS_READY))
				.orderByDesc(OzonShipmentAct::getId)
				.last("LIMIT 1");
		return this.selectOne(wrapper);
	}
}
