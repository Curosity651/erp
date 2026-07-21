package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调拨单实体
 *
 * @author erp
 */
@Data
@TableName("wms_transfer_order")
@Schema(title = "调拨单实体")
public class TransferOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调拨单号")
	private String transferNo;

	@Schema(title = "源仓库ID")
	private Long fromWarehouseId;

	@Schema(title = "目标仓库ID")
	private Long toWarehouseId;

	@Schema(title = "调拨类型: NORMAL-普通调拨 / FBO_INBOUND-FBO入库")
	private String transferType;

	@Schema(title = "出库时间")
	private LocalDateTime shipTime;

	@Schema(title = "结束时间（完成/取消/撤回）")
	private LocalDateTime endTime;

	@Schema(title = "状态: DRAFT-草稿 / IN_TRANSIT-在途 / COMPLETED-已入库 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@Version
	@Schema(title = "乐观锁版本号")
	private Integer version;

}
