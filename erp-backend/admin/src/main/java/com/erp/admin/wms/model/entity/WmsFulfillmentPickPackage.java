package com.erp.admin.wms.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wms_fulfillment_pick_package")
public class WmsFulfillmentPickPackage {
	@TableId(type = IdType.AUTO)
	private Long id;
	private String batchNo;
	private Long taskId;
	private String snapshotHash;
	private String warehouseFileName;
	private String warehouseObjectKey;
	private String warehouseSha256;
	private String archiveFileName;
	private String archiveObjectKey;
	private String archiveSha256;
	private Integer orderCount;
	private Integer totalQuantity;
	private String packageStatus;
	private String errorMessage;
	private Long createBy;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
}
