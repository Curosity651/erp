package com.erp.admin.wms.mapper;

import com.erp.admin.wms.converter.PlatformRegionMappingConverter;
import com.erp.admin.wms.model.entity.PlatformRegionMapping;
import com.erp.admin.wms.model.vo.PlatformRegionMappingVO;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;
import java.util.stream.Collectors;

public interface PlatformRegionMappingMapper extends ExtendMapper<PlatformRegionMapping> {

    default List<PlatformRegionMappingVO> selectByRegionId(Long regionId) {
        LambdaQueryWrapperX<PlatformRegionMapping> wrapper = WrappersX.lambdaQueryX(PlatformRegionMapping.class)
                .eq(PlatformRegionMapping::getRegionId, regionId);
        return this.selectList(wrapper).stream()
                .map(PlatformRegionMappingConverter.INSTANCE::poToVo)
                .collect(Collectors.toList());
    }

    default List<String> selectPlatformsByRegionId(Long regionId) {
        LambdaQueryWrapperX<PlatformRegionMapping> wrapper = WrappersX.lambdaQueryX(PlatformRegionMapping.class)
                .eq(PlatformRegionMapping::getRegionId, regionId);
        return this.selectList(wrapper).stream()
                .map(PlatformRegionMapping::getPlatform)
                .collect(Collectors.toList());
    }

    default boolean existsByRegionId(Long regionId) {
        LambdaQueryWrapperX<PlatformRegionMapping> wrapper = WrappersX.lambdaQueryX(PlatformRegionMapping.class)
                .eq(PlatformRegionMapping::getRegionId, regionId);
        return this.selectCount(wrapper) > 0;
    }

    default PlatformRegionMapping selectByPlatform(String platform) {
        LambdaQueryWrapperX<PlatformRegionMapping> wrapper = WrappersX.lambdaQueryX(PlatformRegionMapping.class)
                .eq(PlatformRegionMapping::getPlatform, platform);
        return this.selectOne(wrapper);
    }

    default List<PlatformRegionMapping> selectByRegionIds(List<Long> regionIds) {
        LambdaQueryWrapperX<PlatformRegionMapping> wrapper = WrappersX.lambdaQueryX(PlatformRegionMapping.class)
                .in(PlatformRegionMapping::getRegionId, regionIds);
        return this.selectList(wrapper);
    }

    default List<PlatformRegionMappingVO> selectAll() {
        return this.selectList(null).stream()
                .map(PlatformRegionMappingConverter.INSTANCE::poToVo)
                .collect(Collectors.toList());
    }
}
