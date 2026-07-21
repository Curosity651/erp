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
import java.math.BigDecimal;

/**
 * 仓库实体
 *
 * @author erp
 * @since 2024-12-22
 */
@Data
@TableName("wms_warehouse")
@Schema(title = "仓库实体")
public class Warehouse {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "仓库编码 (手动输入)")
	private String warehouseCode;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "仓库类型: OWN-自有仓 / FBO-FBO仓")
	private String warehouseType;

	@Schema(title = "所属区域ID")
	private Long regionId;

	@Schema(title = "关联平台: ozon|wildberries|yandex")
	private String platform;

	@Schema(title = "平台仓库ID")
	private String platformWarehouseId;

	@Schema(title = "关联店铺ID（FBO仓库使用，自有仓库为空）")
	private Long shopId;

	@Schema(title = "仓库地址")
	private String address;

	@Schema(title = "联系人")
	private String contactName;

	@Schema(title = "联系电话")
	private String contactPhone;

	@Schema(title = "状态: 1-启用 / 0-停用")
	private Integer status;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "排数(结构参数)")
	private Integer rackRows;

	@Schema(title = "每排列数")
	private Integer rackColumns;

	@Schema(title = "排号前缀(A→A1;空=纯数字)")
	private String rackNoPrefix;

	@Schema(title = "列号补零位宽(2→03)")
	private Integer codePadWidth;

	@Schema(title = "默认库位类型 BIG/SMALL")
	private String defaultLocationType;

	@Schema(title = "库位已生成幂等标记 1是/0否")
	private Integer locationGenerated;

	@Schema(title = "Pallet levels for each two-dimensional location")
	private Integer palletLevels;

	@Schema(title = "Maximum distinct owner and SKU combinations per pallet")
	private Integer maxSkuKindsPerPallet;

	@Schema(title = "Whether different owners may share a mixed pallet")
	private Integer allowCrossOwnerMix;

	private Integer defaultPalletLengthMm;

	private Integer defaultPalletWidthMm;

	private Integer defaultPalletHeightMm;

	private BigDecimal defaultPalletMaxWeightKg;

	private BigDecimal defaultPalletUtilization;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新人")
	private Long updateBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
