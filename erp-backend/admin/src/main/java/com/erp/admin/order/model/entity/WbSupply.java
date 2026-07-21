package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * WB 发货批次表
 *
 * @author erp 2025-09-27 23:16:08
 */
@Data
@TableName("wb_supply")
@Schema(title = "WB 发货批次表")
public class WbSupply {

	/**
	 * 主键
	 */
	@TableId
	@Schema(title="主键")
	private Long id;
    
	/**
	 * 店铺ID (关联 shop.id)
	 */
	@Schema(title="店铺ID (关联 shop.id)")
	private Long shopId;
    
	/**
	 * 平台: wildberries
	 */
	@Schema(title="平台: wildberries")
	private String platform;
    
	/**
	 * 平台的 supplyId
	 */
	@Schema(title="平台的 supplyId")
	private String supplyId;
    
	/**
	 * 供货批次名称
	 */
	@Schema(title="供货批次名称")
	private String name;
    
	/**
	 * 面单数据
	 */
	@Schema(title="面单数据")
	private String labelBase64;
    
	/**
	 * 平台创建时间
	 */
	@Schema(title="平台创建时间")
	private LocalDateTime createdAt;
    
	/**
	 * 最后一次同步时间
	 */
	@Schema(title="最后一次同步时间")
	private LocalDateTime syncedAt;
    
	/**
	 * 记录创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(title="记录创建时间")
	private LocalDateTime createTime;
    
	/**
	 * 记录更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title="记录更新时间")
	private LocalDateTime updateTime;
    

}
