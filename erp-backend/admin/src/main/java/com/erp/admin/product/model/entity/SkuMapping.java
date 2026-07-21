package com.erp.admin.product.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SKU 映射实体。
 *
 * <p>按最新设计，`sku_mapping` 仅表达“当前有效映射”，删除语义改为物理删除，
 * 因此这里不再保留逻辑删除字段。</p>
 */
@Data
@TableName("sku_mapping")
@Schema(title = "SKU映射")
public class SkuMapping {

	@TableId
	@Schema(title = "ID")
	private Long id;

	@Schema(title = "平台商品ID")
	private String platformItemId;

	@Schema(title = "ERP SKU编码")
	private String skuCode;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
