package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存调整单实体
 *
 * @author erp
 */
@Data
@TableName("wms_adjustment_order")
@Schema(title = "库存调整单实体")
public class AdjustmentOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调整单号（AD + 年月日 + 4位序号）")
	private String adjustmentNo;

	@Schema(title = "调整仓库ID")
	private Long warehouseId;

	@Schema(title = "货主（货物归属），整单归属一个货主；报废需通知该货主确认")
	private Long erpTenantId;

	@Schema(title = "调整类型：恒为 SCRAP-报废")
	private String adjustmentType;

	@Schema(title = "调整日期")
	private LocalDate adjustmentDate;

	@Schema(title = "调整原因")
	private String adjustmentReason;

	@Schema(title = "状态: PENDING_OWNER-待货主确认 / PENDING_DESTROY-待仓库销毁 / SCRAPPED-已销毁 / REJECTED-已驳回 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "仓库实际销毁时间")
	private LocalDateTime confirmTime;

	@Schema(title = "仓库实际销毁操作人")
	private Long confirmBy;

	@Schema(title = "货主确认/驳回时间")
	private LocalDateTime ownerActionTime;

	@Schema(title = "货主确认/驳回操作人")
	private Long ownerActionBy;

	@Schema(title = "货主驳回原因")
	private String rejectReason;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
