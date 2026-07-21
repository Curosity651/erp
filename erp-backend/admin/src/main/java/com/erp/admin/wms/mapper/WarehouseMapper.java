package com.erp.admin.wms.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.converter.WarehouseConverter;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.WarehouseTypeEnum;
import com.erp.admin.wms.model.qo.WarehouseQO;
import com.erp.admin.wms.model.vo.WarehouseOptionVO;
import com.erp.admin.wms.model.vo.WarehousePageVO;
import org.ballcat.common.core.constant.enums.BooleanEnum;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 仓库Mapper
 *
 * @author erp
 */
public interface WarehouseMapper extends ExtendMapper<Warehouse> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<WarehousePageVO> VO分页数据
	 */
	default PageResult<WarehousePageVO> queryPage(PageParam pageParam, WarehouseQO qo) {
		IPage<Warehouse> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
			.likeIfPresent(Warehouse::getWarehouseCode, qo.getWarehouseCode())
			.likeIfPresent(Warehouse::getWarehouseName, qo.getWarehouseName())
			.eqIfPresent(Warehouse::getWarehouseType, qo.getWarehouseType())
			.eqIfPresent(Warehouse::getRegionId, qo.getRegionId())
			.eqIfPresent(Warehouse::getStatus, qo.getStatus())
			.orderByDesc(Warehouse::getId);
		this.selectPage(page, wrapper);
		IPage<WarehousePageVO> voPage = page.convert(WarehouseConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取仓库下拉选项列表
	 * @return List<WarehouseOptionVO> 仓库下拉选项列表
	 */
	default List<WarehouseOptionVO> selectWarehouseOptions() {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
			.select(Warehouse::getId, Warehouse::getWarehouseCode, Warehouse::getWarehouseName, Warehouse::getWarehouseType, Warehouse::getRegionId)
			.eq(Warehouse::getStatus, BooleanEnum.TRUE.intValue()) // 只查询启用状态的仓库
			.orderByAsc(Warehouse::getWarehouseCode); // 按编码排序

		List<Warehouse> warehouses = this.selectList(wrapper);

		return warehouses.stream()
			.map(WarehouseConverter.INSTANCE::poToOptionVo)
			.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 更新仓库状态
	 * @param id 仓库ID
	 * @param status 状态: 1-启用 / 0-停用
	 * @return 影响行数
	 */
	default int updateStatus(Long id, Integer status) {
		LambdaUpdateWrapper<Warehouse> updateWrapper = Wrappers.lambdaUpdate(Warehouse.class)
				.eq(Warehouse::getId, id)
				.set(Warehouse::getStatus, status);
		return this.update(null, updateWrapper);
	}

	/**
	 * 根据仓库ID集合查询仓库名称映射
	 * @param warehouseIds 仓库ID集合
	 * @return Map<仓库ID, 仓库名称>
	 */
	default java.util.Map<Long, String> selectWarehouseNameMap(java.util.Set<Long> warehouseIds) {
		if (warehouseIds == null || warehouseIds.isEmpty()) {
			return java.util.Collections.emptyMap();
		}
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.select(Warehouse::getId, Warehouse::getWarehouseName)
				.in(Warehouse::getId, warehouseIds);
		return this.selectList(wrapper).stream()
				.collect(java.util.stream.Collectors.toMap(Warehouse::getId, Warehouse::getWarehouseName));
	}

	/**
	 * 查询自有仓库列表
	 * <p>
	 * 用于库存预测，只查询自有仓库（非第三方仓）
	 *
	 * @param warehouseId 可选，指定仓库ID进一步筛选
	 * @return 自有仓库列表
	 */
	default List<Warehouse> selectOwnWarehouses(Long warehouseId) {
		return selectList(WrappersX.<Warehouse>lambdaQueryX()
				.eq(Warehouse::getWarehouseType, WarehouseTypeEnum.OWN.getCode())
				.eqIfPresent(Warehouse::getId, warehouseId)
				.eq(Warehouse::getStatus, 1));
	}

	/**
	 * 根据平台和平台仓库ID查询仓库
	 * @param platform 平台
	 * @param platformWarehouseId 平台仓库ID
	 * @return 仓库实体，不存在返回 null
	 */
	default Warehouse selectByPlatformWarehouseId(String platform, String platformWarehouseId) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.eq(Warehouse::getPlatform, platform)
				.eq(Warehouse::getPlatformWarehouseId, platformWarehouseId);
		return this.selectOne(wrapper);
	}

	/**
	 * 根据平台、店铺ID和平台仓库ID查询仓库
	 * <p>
	 * 用于 FBO 仓库查询，通过 (platform, shopId, platformWarehouseId) 唯一定位
	 *
	 * @param platform 平台
	 * @param shopId 店铺ID
	 * @param platformWarehouseId 平台仓库ID
	 * @return 仓库实体，不存在返回 null
	 */
	default Warehouse selectByPlatformShopAndWarehouseId(String platform, Long shopId, String platformWarehouseId) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.eq(Warehouse::getPlatform, platform)
				.eq(Warehouse::getShopId, shopId)
				.eq(Warehouse::getPlatformWarehouseId, platformWarehouseId);
		return this.selectOne(wrapper);
	}

	/**
	 * 查询平台下已存在的平台仓库ID集合
	 * @param platform 平台
	 * @return 已存在的平台仓库ID集合
	 */
	default java.util.Set<String> selectExistingPlatformWarehouseIds(String platform) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.eq(Warehouse::getPlatform, platform)
				.eq(Warehouse::getWarehouseType, WarehouseTypeEnum.FBO.getCode())
				.select(Warehouse::getPlatformWarehouseId);
		return this.selectList(wrapper).stream()
				.map(Warehouse::getPlatformWarehouseId)
				.collect(java.util.stream.Collectors.toSet());
	}

	/**
	 * 查询指定平台 FBO 仓库的最大编码
	 * @param codePrefix 编码前缀，如 "FBO-OZON-"
	 * @return 最大编码的仓库，不存在返回 null
	 */
	default Warehouse selectMaxCodeByPrefix(String codePrefix) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.likeRight(Warehouse::getWarehouseCode, codePrefix)
				.orderByDesc(Warehouse::getWarehouseCode)
				.last("LIMIT 1");
		return this.selectOne(wrapper);
	}

	/**
	 * 查询指定区域下的仓库数量
	 * @param regionId 区域ID
	 * @return 仓库数量
	 */
	default int countByRegionId(Long regionId) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.eq(Warehouse::getRegionId, regionId);
		return Math.toIntExact(this.selectCount(wrapper));
	}

	/**
	 * 批量查询多个区域的仓库列表（用于统计仓库数量）
	 * @param regionIds 区域ID列表
	 * @return 包含 regionId 的仓库列表
	 */
	default List<Warehouse> selectByRegionIds(java.util.Collection<Long> regionIds) {
		if (regionIds == null || regionIds.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.select(Warehouse::getRegionId)
				.in(Warehouse::getRegionId, regionIds);
		return this.selectList(wrapper);
	}

	/**
	 * 检查仓库编码是否存在
	 * @param warehouseCode 仓库编码
	 * @param excludeId 排除的ID（更新时使用），可为 null
	 * @return 存在的数量
	 */
	default long countByWarehouseCode(String warehouseCode, Long excludeId) {
		LambdaQueryWrapperX<Warehouse> wrapper = WrappersX.lambdaQueryX(Warehouse.class)
				.eq(Warehouse::getWarehouseCode, warehouseCode)
				.neIfPresent(Warehouse::getId, excludeId);
		return this.selectCount(wrapper);
	}

	/**
	 * 查询区域内自有仓库列表
	 */
	default List<Warehouse> selectOwnByRegion(Long regionId) {
		return selectList(WrappersX.<Warehouse>lambdaQueryX()
				.eq(Warehouse::getRegionId, regionId)
				.eq(Warehouse::getWarehouseType, WarehouseTypeEnum.OWN.getCode())
				.eq(Warehouse::getStatus, 1));
	}

	/**
	 * 按区域 ID 集合查询自有仓库列表
	 */
	default List<Warehouse> selectOwnByRegionIds(java.util.Collection<Long> regionIds) {
		if (regionIds == null || regionIds.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		return selectList(WrappersX.<Warehouse>lambdaQueryX()
				.in(Warehouse::getRegionId, regionIds)
				.eq(Warehouse::getWarehouseType, WarehouseTypeEnum.OWN.getCode())
				.eq(Warehouse::getStatus, 1));
	}

}