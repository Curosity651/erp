package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * 服务商「仓储概览」聚合视图。
 *
 * <p>作用域：分配容量按有效排分配统计，库存按 location_inventory.wms_tenant_id 和货主父服务商统计。
 * 公共暂存库存计入在库件数，但不计入分配容量。纯只读。
 *
 * @author erp
 */
@Data
public class StorageOverviewVO {

	/** 我的仓库数（有 active 货架分配的仓）。 */
	private Integer warehouseCount;

	/** 已分配排数（active 分配记录数）。 */
	private Integer rackCount;

	/** 分配库位数（分配货架下的库位总数）。 */
	private Long allocatedLocations;

	/** 已占用库位（分配货架下有货的库位数）。 */
	private Long occupiedLocations;

	/** 占用率（百分比，一位小数；分配库位为 0 时为 0）。 */
	private BigDecimal occupancyRate;

	/** 在库总件数（包含公共暂存库存）。 */
	private Long onHandQty;

	/** 仓库明细列表。 */
	private List<WarehouseRow> warehouses;

	/** 单仓行。 */
	@Data
	public static class WarehouseRow {

		private Long warehouseId;

		private String warehouseName;

		private String warehouseCode;

		private String regionName;

		private Integer rackCount;

		private Long allocatedLocations;

		private Long occupiedLocations;

		private BigDecimal occupancyRate;

		private Long onHandQty;

	}

	/** 货架维度行。 */
	@Data
	public static class RackRow {

		private String rackNo;

		private Long locationCount;

		private Long occupiedCount;

		private BigDecimal occupancyRate;

		private BigDecimal monthlyFee;

		private LocalDate effectiveTo;

		private Boolean expiringSoon;

	}

	/** 货主维度行。 */
	@Data
	public static class OwnerRow {

		private Long erpTenantId;

		private String ownerName;

		private Long occupiedLocations;

		private Long onHandQty;

		private Integer skuCount;

		/** 占本仓比例（按在库件数，百分比一位小数）。 */
		private BigDecimal sharePct;

	}

}
