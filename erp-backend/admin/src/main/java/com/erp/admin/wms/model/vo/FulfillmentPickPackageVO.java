package com.erp.admin.wms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FulfillmentPickPackageVO {
	private String batchNo;
	private Long taskId;
	private String snapshotHash;
	private String warehouseFileName;
	private String warehouseDownloadUrl;
	private String warehouseSha256;
	private String archiveFileName;
	private String archiveDownloadUrl;
	private String archiveSha256;
	private Integer orderCount;
	private Integer totalQuantity;
	private LocalDateTime generatedTime;
}
