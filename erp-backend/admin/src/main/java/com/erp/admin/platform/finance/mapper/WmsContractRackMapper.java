package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsContractRack;
import com.erp.admin.platform.finance.model.vo.ContractRackBindingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WmsContractRackMapper extends ExtendMapper<WmsContractRack> {

    @Select("SELECT cr.contract_id AS contractId, c.contract_no AS contractNo, "
            + "cr.rack_assignment_id AS rackAssignmentId, cr.rack_no AS rackNo, "
            + "c.wms_tenant_id AS wmsTenantId, c.start_date AS effectiveFrom, "
            + "c.end_date AS effectiveTo, c.monthly_rent_per_unit AS monthlyFee, "
            + "c.contract_status AS contractStatus, c.payment_status AS paymentStatus "
            + "FROM wms_contract_rack cr "
            + "JOIN wms_service_contract c ON c.id = cr.contract_id "
            + "WHERE c.warehouse_id = #{warehouseId} "
            + "AND ((c.contract_status = 'DRAFT' AND c.payment_status = 'PENDING') "
            + "OR (c.contract_status = 'ACTIVE' AND c.payment_status = 'PAID'))")
    List<ContractRackBindingVO> listCurrentBindingsByWarehouse(@Param("warehouseId") Long warehouseId);

    @Select("<script>"
            + "SELECT COUNT(*) FROM wms_contract_rack cr "
            + "JOIN wms_service_contract c ON c.id = cr.contract_id "
            + "WHERE c.warehouse_id = #{warehouseId} AND cr.rack_no = #{rackNo} "
            + "AND ((c.contract_status = 'DRAFT' AND c.payment_status = 'PENDING') "
            + "OR (c.contract_status = 'ACTIVE' AND c.payment_status = 'PAID')) "
            + "AND c.start_date &lt;= COALESCE(#{effectiveTo}, '9999-12-31') "
            + "AND c.end_date &gt;= #{effectiveFrom} "
            + "<if test='excludeContractId != null'>AND c.id != #{excludeContractId}</if>"
            + "</script>")
    long countOverlappingContractBindings(@Param("warehouseId") Long warehouseId,
            @Param("rackNo") String rackNo, @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("excludeContractId") Long excludeContractId);

    @Select("SELECT COUNT(*) FROM wms_contract_rack WHERE rack_assignment_id = #{assignmentId}")
    long countAssignmentLinks(@Param("assignmentId") Long assignmentId);

}
