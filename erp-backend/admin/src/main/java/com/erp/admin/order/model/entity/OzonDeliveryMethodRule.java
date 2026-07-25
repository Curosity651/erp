package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ozon_delivery_method_rule")
public class OzonDeliveryMethodRule {
	@TableId
	private Long id;
	private Long shopId;
	private Long deliveryMethodId;
	private Integer actRequired;
	private Integer containersCount;
	private Integer enabled;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
	@TableField(exist = false)
	private String shopName;
	@TableField(exist = false)
	private String deliveryMethodName;
}
