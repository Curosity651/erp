package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 采购单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购单查询对象")
public class PurchaseOrderQO {

	@Schema(title = "采购单号（模糊搜索）")
	private String orderNo;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "采购单状态")
	private String orderStatus;

	@Schema(title = "发货状态")
	private String shippingStatus;

	@Schema(title = "入库状态")
	private String receivingStatus;

	@Schema(title = "首付款状态: 0-未付 / 1-已付")
	private Integer prepayStatus;

	@Schema(title = "尾款状态: 0-未付 / 1-已付")
	private Integer balanceStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "下单日期开始")
	private LocalDate orderDateStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "下单日期结束")
	private LocalDate orderDateEnd;

	@Schema(title = "SKU编码（关联查询）")
	private String skuCode;

}
