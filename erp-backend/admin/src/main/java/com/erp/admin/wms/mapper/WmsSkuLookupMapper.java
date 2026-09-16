package com.erp.admin.wms.mapper;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 作业用 SKU 速查 Mapper（数据级可见性 ②）。
 *
 * <p>整个 Mapper 标 {@code @InterceptorIgnore(tenantLine = "true")} 绕过 ERP 租户行注入， 作用域改由
 * {@code WmsSkuLookupService} 按身份显式控制（平台=全部、服务商=名下货主、货主=自己）。
 *
 * @author erp
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface WmsSkuLookupMapper {

	String COLS = "sku_code, tenant_id AS erp_tenant_id, chinese_name, russian_name, "
			+ "weight, weight_unit, package_length, package_width, package_height, package_unit, quantity_per_pallet, needs_power";

	/**
	 * 跨全部货主按 SKU 编码查（平台超管作业用）。
	 */
	@Select("SELECT " + COLS + " FROM sku WHERE sku_code = #{skuCode}")
	List<SkuLookupVO> findBySkuCodeAllTenants(@Param("skuCode") String skuCode);

	/**
	 * 限定货主集合按 SKU 编码查（服务商=名下货主 / 货主=自己）。
	 */
	@Select("<script>SELECT " + COLS + " FROM sku WHERE sku_code = #{skuCode} "
			+ "AND tenant_id IN <foreach collection='tenantIds' item='t' open='(' separator=',' close=')'>#{t}</foreach>"
			+ "</script>")
	List<SkuLookupVO> findBySkuCodeInTenants(@Param("skuCode") String skuCode,
			@Param("tenantIds") Collection<Long> tenantIds);

	@Select("SELECT " + COLS + " FROM sku WHERE tenant_id = #{erpTenantId} "
			+ "AND sku_code = #{skuCode} LIMIT 1")
	SkuLookupVO findByTenantAndSku(@Param("erpTenantId") Long erpTenantId, @Param("skuCode") String skuCode);

	@Select("<script>SELECT CONCAT(t.warehouse_sku_prefix, '-', s.sku_code) AS warehouse_sku_code, "
			+ "t.tenant_name AS owner_name, s.sku_code, s.tenant_id AS erp_tenant_id, "
			+ "s.chinese_name, s.russian_name, s.weight, s.weight_unit, s.package_length, "
			+ "s.package_width, s.package_height, s.package_unit, s.quantity_per_pallet, s.needs_power "
			+ "FROM sku s INNER JOIN sys_tenant t ON t.id = s.tenant_id "
			+ "WHERE UPPER(CONCAT(t.warehouse_sku_prefix, '-', s.sku_code)) IN "
			+ "<foreach collection='warehouseSkuCodes' item='code' open='(' separator=',' close=')'>#{code}</foreach>"
			+ "</script>")
	List<SkuLookupVO> findByWarehouseSkuCodes(@Param("warehouseSkuCodes") Collection<String> warehouseSkuCodes);

}
