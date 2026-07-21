package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsPallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

@Mapper
public interface WmsPalletMapper extends ExtendMapper<WmsPallet> {
    @Select("SELECT * FROM wms_pallet WHERE id = #{id} FOR UPDATE")
    WmsPallet selectForUpdate(@Param("id") Long id);
}

