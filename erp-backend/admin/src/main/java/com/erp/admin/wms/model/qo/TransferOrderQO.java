package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 调拨单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "调拨单查询对象")
public class TransferOrderQO {

	@Schema(title = "调拨单号")
	private String transferNo;

	@Schema(title = "调拨类型: NORMAL-普通调拨 / FBO_INBOUND-FBO入库")
	private String transferType;

	@Schema(title = "源仓库ID")
	private Long fromWarehouseId;

	@Schema(title = "目标仓库ID")
	private Long toWarehouseId;

	@Schema(title = "状态: DRAFT-草稿 / IN_TRANSIT-在途 / COMPLETED-已入库 / CANCELLED-已取消")
	private String orderStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(title = "创建时间起始")
	private LocalDateTime createTimeStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(title = "创建时间结束")
	private LocalDateTime createTimeEnd;

}
