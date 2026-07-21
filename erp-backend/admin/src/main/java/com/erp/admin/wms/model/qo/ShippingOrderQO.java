package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 物流单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "物流单查询对象")
public class ShippingOrderQO {

	@Schema(title = "物流单号")
	private String shippingNo;

	@Schema(title = "物流商ID")
	private Long providerId;

	@Schema(title = "物流单状态")
	private String shippingStatus;

	@Schema(title = "付款状态")
	private Integer paymentStatus;

	@Schema(title = "物流方式")
	private String shippingMethod;

	@Schema(title = "物流线路")
	private String shippingRoute;

	@Schema(title = "目标区域ID")
	private Long targetRegionId;

	@Schema(title = "SKU编码")
	private String skuCode;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "发货日期起始")
	private LocalDate shippingDateStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "发货日期结束")
	private LocalDate shippingDateEnd;

}
