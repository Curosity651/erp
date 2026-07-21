package com.erp.admin.wms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 货架分配记录（C2）。平台超管把仓库某排分配给 WMS 服务商。
 *
 * <p>合同流水：无逻辑删除，结束分配用 {@code effectiveTo}；无租户行级隔离（平台级，按 wmsTenantId 显式过滤）。
 *
 * @author erp
 */
@Data
@TableName("wms_rack_assignment")
@Schema(title = "货架分配")
public class WmsRackAssignment {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键")
	private Long id;

	@Schema(title = "被分配的 WMS 服务商 tenant_id")
	private Long wmsTenantId;

	@Schema(title = "所在仓库")
	private Long warehouseId;

	@Schema(title = "货架排号")
	private String rackNo;

	@Schema(title = "月租金(CNY)")
	private BigDecimal monthlyFee;

	@Schema(title = "生效日期")
	private LocalDate effectiveFrom;

	@Schema(title = "结束日期(NULL=当前有效)")
	private LocalDate effectiveTo;

	@Schema(title = "合同附件URL")
	private String contractFileUrl;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "操作超管 user_id")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
