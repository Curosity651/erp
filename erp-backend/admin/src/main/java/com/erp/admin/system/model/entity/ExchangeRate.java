package com.erp.admin.system.model.entity;

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
 * 每日汇率表(用于订单金额折算为人民币)
 *
 * @author system
 */
@Data
@TableName("exchange_rate")
@Schema(title = "每日汇率表")
public class ExchangeRate {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	/**
	 * 基准币种(base),例如:RUB、BYN、USD
	 */
	@Schema(title = "基准币种")
	private String baseCurrencyCode;

	/**
	 * 目标币种(target),通常为CNY
	 */
	@Schema(title = "目标币种")
	private String targetCurrencyCode;

	/**
	 * 汇率,表示1单位base_currency = rate单位target_currency
	 */
	@Schema(title = "汇率")
	private BigDecimal rate;

	/**
	 * 汇率生效日期(对应Fixer.io返回的date字段)
	 */
	@Schema(title = "汇率生效日期")
	private LocalDate rateDate;

	/**
	 * Fixer返回的时间戳(秒级)
	 */
	@Schema(title = "API时间戳")
	private Long apiTimestamp;

	/**
	 * 汇率来源平台,如Fixer.io、OpenExchangeRates等
	 */
	@Schema(title = "汇率来源平台")
	private String source;

	/**
	 * 备注信息,可用于记录异常或回退说明
	 */
	@Schema(title = "备注信息")
	private String remark;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createdAt;

	/**
	 * 最后更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "最后更新时间")
	private LocalDateTime updatedAt;

}
