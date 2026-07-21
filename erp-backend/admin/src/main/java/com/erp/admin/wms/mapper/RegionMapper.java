package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.converter.RegionConverter;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.qo.RegionQO;
import com.erp.admin.wms.model.vo.RegionOptionVO;
import com.erp.admin.wms.model.vo.RegionPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;
import java.util.stream.Collectors;

public interface RegionMapper extends ExtendMapper<Region> {

    default PageResult<RegionPageVO> queryPage(PageParam pageParam, RegionQO qo) {
        IPage<Region> page = this.prodPage(pageParam);
        LambdaQueryWrapperX<Region> wrapper = WrappersX.lambdaQueryX(Region.class)
                .likeIfPresent(Region::getRegionCode, qo.getRegionCode())
                .likeIfPresent(Region::getRegionName, qo.getRegionName())
                .eqIfPresent(Region::getStatus, qo.getStatus())
                .orderByDesc(Region::getId);
        this.selectPage(page, wrapper);
        IPage<RegionPageVO> voPage = page.convert(RegionConverter.INSTANCE::poToPageVo);
        return new PageResult<>(voPage.getRecords(), voPage.getTotal());
    }

    default List<RegionOptionVO> selectRegionOptions() {
        LambdaQueryWrapperX<Region> wrapper = WrappersX.lambdaQueryX(Region.class)
                .eq(Region::getStatus, 1)
                .orderByAsc(Region::getRegionCode);
        return this.selectList(wrapper).stream()
                .map(RegionConverter.INSTANCE::poToOptionVo)
                .collect(Collectors.toList());
    }

    default Region selectByCode(String regionCode) {
        LambdaQueryWrapperX<Region> wrapper = WrappersX.lambdaQueryX(Region.class)
                .eq(Region::getRegionCode, regionCode);
        return this.selectOne(wrapper);
    }

    /**
     * 查询所有启用的区域
     */
    default List<Region> selectEnabledList() {
        LambdaQueryWrapperX<Region> wrapper = WrappersX.lambdaQueryX(Region.class)
                .eq(Region::getStatus, 1)
                .orderByAsc(Region::getRegionCode);
        return this.selectList(wrapper);
    }
}
