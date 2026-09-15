package com.erp.admin.wms.pickpackage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PickTaskPackageFiles {
	private String warehouseFileName;
	private byte[] warehouseZip;
	private String warehouseSha256;
	private String archiveFileName;
	private byte[] archiveZip;
	private String archiveSha256;
}
