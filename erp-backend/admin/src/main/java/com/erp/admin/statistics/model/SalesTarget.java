package com.erp.admin.statistics.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.admin.statistics.model.entity.enums.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售目标
 *
 * @author erp 2025-10-25 21:40:57
 */
@Data
@TableName("sales_target")
@Schema(title = "销售目标")
public class SalesTarget {

	/**
	 * ID
	 */
	@TableId
	@Schema(title = "ID")
	private Long id;

	/**
	 * 目标类型：年度或月度
	 */
	@Schema(title = "目标类型：年度或月度")
	private TargetType targetType;

	/**
	 * 目标年份
	 */
	@Schema(title = "目标年份")
	private Integer targetYear;

	/**
	 * 目标月份，仅当 MONTHLY 时有值
	 */
	@Schema(title = "目标月份，仅当 MONTHLY 时有值")
	private Integer targetMonth;

	/**
	 * 目标销售额
	 */
	@Schema(title = "目标销售额")
	private BigDecimal targetAmount;

	/**
	 * 货币单位
	 */
	@Schema(title = "货币单位")
	private String currency;

	/**
	 * 创建人
	 */
	@Schema(title = "创建人")
	private Long createdBy;

	/**
	 * 备注
	 */
	@Schema(title = "备注")
	private String remark;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;


}
