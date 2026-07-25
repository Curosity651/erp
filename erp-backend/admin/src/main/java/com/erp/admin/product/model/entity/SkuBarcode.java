package com.erp.admin.product.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sku_barcode")
public class SkuBarcode {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long tenantId;
	private Long skuId;
	private String skuCode;
	private String barcode;
	private String barcodeType;
	private Integer primaryFlag;
	private Integer enabled;
	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createTime;
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

}

