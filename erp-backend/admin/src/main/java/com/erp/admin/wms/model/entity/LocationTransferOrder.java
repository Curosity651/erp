package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库位调整单（库内移库单）实体。
 *
 * @author erp
 */
@Data
@TableName("wms_location_transfer_order")
@Schema(title = "库位调整单实体")
public class LocationTransferOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调整单号（LT + 年月日 + 4位序号）")
	private String transferNo;

	@Schema(title = "仓库ID")
	private Long warehouseId;

	@Schema(title = "货主（货物归属），整单归属一个货主")
	private Long erpTenantId;

	@Schema(title = "状态：PENDING-待调整 / COMPLETED-已完成 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "调整完成时间")
	private LocalDateTime completeTime;

	@Schema(title = "调整完成操作人")
	private Long completeBy;

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
