package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退货质检明细（对账/理赔依据）。收货时按 SKU 建行，质检时回填结果。
 *
 * @author erp
 */
@Data
@TableName("wms_return_qc_item")
@Schema(title = "退货质检明细")
public class WmsReturnQcItem {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "退货单ID")
    private Long returnOrderId;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "是否电子类")
    private Integer electronic;

    @Schema(title = "应退数")
    private Integer expectedQty;

    @Schema(title = "实收数")
    private Integer receivedQty;

    @Schema(title = "良品数量")
    private Integer qualifiedQty;

    @Schema(title = "残次品数量")
    private Integer damagedQty;

    @Schema(title = "良品回库分区")
    private String qualifiedZone;

    @Schema(title = "良品回库库位")
	private String qualifiedLocationCode;

	private Long qualifiedLocationId;

    private Long qualifiedPalletId;

    private Long qualifiedSlotId;

    private String qualifiedSlotCode;

    @Schema(title = "残次品回库库位")
	private String damagedLocationCode;

	private Long damagedLocationId;

    private Long damagedPalletId;

    private Long damagedSlotId;

    private String damagedSlotCode;

    @Schema(title = "质检结果 PASS/FAIL")
    private String qcResult;

    @Schema(title = "回库分区 RETURN/DEFECTIVE")
    private String zone;

    @Schema(title = "品质 GOOD/DAMAGED")
    private String quality;

    @Schema(title = "回库库位")
    private String locationCode;

    @Schema(title = "质检备注")
    private String qcRemark;

    @Schema(title = "质检照片OSS文件ID(sys_file.id，逗号分隔)")
    private String qcPhotos;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
