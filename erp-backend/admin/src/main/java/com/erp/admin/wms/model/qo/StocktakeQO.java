package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 盘点单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点单查询对象")
public class StocktakeQO {

	@Schema(title = "盘点单号")
	private String stocktakeNo;

	@Schema(title = "盘点仓库ID")
	private Long warehouseId;

	@Schema(title = "状态: DRAFT-草稿 / IN_PROGRESS-盘点中 / COMPLETED-已完成 / CANCELLED-已取消")
	private String orderStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "盘点日期起始")
	private LocalDate stocktakeDateStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "盘点日期结束")
	private LocalDate stocktakeDateEnd;

}
