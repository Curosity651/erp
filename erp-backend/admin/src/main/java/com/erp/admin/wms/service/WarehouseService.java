package com.erp.admin.wms.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.wms.converter.WarehouseConverter;
import com.erp.admin.wms.mapper.WarehouseMapper;
import com.erp.admin.wms.mapper.WmsRackAssignmentMapper;
import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.enums.WarehouseTypeEnum;
import com.erp.admin.wms.model.qo.WarehouseQO;
import com.erp.admin.wms.model.vo.WarehouseDisplayVO;
import com.erp.admin.wms.model.vo.WarehouseOptionVO;
import com.erp.admin.wms.model.vo.WarehousePageVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 仓库管理服务
 *
 * @author erp
 */
@Slf4j
@Service
public class WarehouseService extends ExtendServiceImpl<WarehouseMapper, Warehouse> {

	@org.springframework.beans.factory.annotation.Autowired
	private WmsRackAssignmentMapper rackAssignmentMapper;

	/**
	 * OWN 仓可见性作用域（P6）：海外仓平台/无上下文→null(看全部)；服务商/货主→其(父)服务商有货架分配的仓库 id 集合。
	 * 服务商与货主均用 {@link WmsTenantContext}（服务商=自身、货主=父服务商）匹配 wms_rack_assignment.wms_tenant_id。
	 * @return 可见 OWN 仓 id 集合；null=不过滤(全部)；空集=看不到
	 */
	private Set<Long> ownWarehouseScope() {
		Long erp = TenantContext.getCurrentTenant();
		if (erp == null || TenantContext.BLOCK_TENANT_ID.equals(erp)) {
			return null;
		}
		Long wms = WmsTenantContext.getCurrentWmsTenant();
		if (wms == null) {
			return Collections.emptySet();
		}
		return new java.util.HashSet<>(rackAssignmentMapper.listWarehouseIdsByWmsTenant(wms));
	}

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<WarehousePageVO> 分页数据
	 */
	public PageResult<WarehousePageVO> queryPage(PageParam pageParam, WarehouseQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取仓库下拉选项列表（按可见性收窄：货主/服务商仅见其服务商有货架分配的 OWN 仓；平台全部）
	 * @return List<WarehouseOptionVO> 仓库下拉选项列表
	 */
	public List<WarehouseOptionVO> getWarehouseOptions() {
		List<WarehouseOptionVO> all = baseMapper.selectWarehouseOptions();
		Set<Long> scope = ownWarehouseScope();
		if (scope == null) {
			return all;
		}
		return all.stream().filter(o -> scope.contains(o.getId())).collect(Collectors.toList());
	}

	/**
	 * 获取仓库详情
	 * @param id 仓库ID
	 * @return WarehousePageVO 仓库详情
	 */
	public WarehousePageVO getDetail(Long id) {
		Warehouse warehouse = this.getById(id);
		return WarehouseConverter.INSTANCE.poToPageVo(warehouse);
	}

	/**
	 * 创建仓库
	 * @param dto 仓库数据传输对象
	 * @return boolean 是否创建成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean createWarehouse(WarehouseDTO dto) {
		// 校验仓库编码唯一性
		this.checkWarehouseCodeUnique(dto.getWarehouseCode(), null);

		Warehouse warehouse = WarehouseConverter.INSTANCE.dtoToEntity(dto);

		return this.save(warehouse);
	}

	/**
	 * 更新仓库
	 * @param dto 仓库数据传输对象
	 * @return boolean 是否更新成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean updateWarehouse(WarehouseDTO dto) {
		Assert.notNull(dto.getId(), "仓库ID不能为空");

		// 校验仓库编码唯一性
		this.checkWarehouseCodeUnique(dto.getWarehouseCode(), dto.getId());

		Warehouse warehouse = WarehouseConverter.INSTANCE.dtoToEntity(dto);
		return this.updateById(warehouse);
	}

	/**
	 * 校验仓库编码唯一性
	 * @param warehouseCode 仓库编码
	 * @param excludeId 排除的ID（更新时使用）
	 */
	private void checkWarehouseCodeUnique(String warehouseCode, Long excludeId) {
		long count = baseMapper.countByWarehouseCode(warehouseCode, excludeId);
		Assert.isTrue(count == 0, "仓库编码已存在");
	}

	/**
	 * 查询指定区域下的仓库数量
	 * @param regionId 区域ID
	 * @return 仓库数量
	 */
	public int countByRegionId(Long regionId) {
		return baseMapper.countByRegionId(regionId);
	}

	/**
	 * 批量查询多个区域的仓库数量（避免 N+1 查询）
	 * @param regionIds 区域ID列表
	 * @return key=regionId, value=仓库数量
	 */
	public Map<Long, Integer> countByRegionIds(List<Long> regionIds) {
		if (CollectionUtils.isEmpty(regionIds)) {
			return Collections.emptyMap();
		}
		List<Warehouse> list = baseMapper.selectByRegionIds(regionIds);
		return list.stream().collect(Collectors.groupingBy(
				Warehouse::getRegionId,
				Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
		));
	}

	/**
	 * 更新仓库状态
	 * @param id 仓库ID
	 * @param status 状态: 1-启用 / 0-停用
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Long id, Integer status) {
		int rows = baseMapper.updateStatus(id, status);
		if (rows == 0) {
			throw new IllegalArgumentException("仓库不存在");
		}
	}

	// ==================== 仓库展示信息填充方法 ====================

	/**
	 * 批量构建仓库展示信息映射
	 *
	 * @param warehouseIds 仓库ID集合
	 * @return 仓库展示信息映射表，key 为 warehouseId
	 */
	public Map<Long, WarehouseDisplayVO> buildDisplayMap(Collection<Long> warehouseIds) {
		if (CollectionUtils.isEmpty(warehouseIds)) {
			return Collections.emptyMap();
		}

		// 过滤空值并去重
		Set<Long> validIds = warehouseIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (validIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 批量查询仓库信息
		List<Warehouse> warehouses = this.listByIds(validIds);

		if (CollectionUtils.isEmpty(warehouses)) {
			return Collections.emptyMap();
		}

		// 构建映射表
		return warehouses.stream()
				.collect(Collectors.toMap(
						Warehouse::getId,
						this::buildWarehouseDisplayVO,
						(v1, v2) -> v1
				));
	}

	/**
	 * 泛型填充方法 - 为 VO 列表填充仓库展示信息
	 * <p>
	 * 使用示例：
	 * <pre>
	 * warehouseService.enrichWarehouseDisplay(
	 *     voList,
	 *     ItemVO::getWarehouseId,
	 *     ItemVO::setWarehouseDisplay
	 * );
	 * </pre>
	 *
	 * @param voList              VO 列表
	 * @param warehouseIdExtractor 仓库ID提取函数
	 * @param displaySetter       仓库展示信息设置函数
	 * @param <T>                 VO 类型
	 */
	public <T> void enrichWarehouseDisplay(List<T> voList,
			Function<T, Long> warehouseIdExtractor,
			BiConsumer<T, WarehouseDisplayVO> displaySetter) {
		if (CollectionUtils.isEmpty(voList)) {
			return;
		}

		// 收集所有仓库ID
		Set<Long> warehouseIds = voList.stream()
				.map(warehouseIdExtractor)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (warehouseIds.isEmpty()) {
			return;
		}

		// 构建映射表
		Map<Long, WarehouseDisplayVO> displayMap = buildDisplayMap(warehouseIds);

		// 填充到 VO
		for (T vo : voList) {
			Long warehouseId = warehouseIdExtractor.apply(vo);
			if (warehouseId != null) {
				WarehouseDisplayVO display = displayMap.get(warehouseId);
				if (display != null) {
					displaySetter.accept(vo, display);
				}
			}
		}
	}

	/**
	 * 构建单个仓库展示信息
	 */
	private WarehouseDisplayVO buildWarehouseDisplayVO(Warehouse warehouse) {
		WarehouseDisplayVO displayVO = new WarehouseDisplayVO();
		displayVO.setId(warehouse.getId());
		displayVO.setWarehouseName(warehouse.getWarehouseName());
		displayVO.setWarehouseType(warehouse.getWarehouseType());
		return displayVO;
	}

	/**
	 * 获取单个仓库展示信息
	 * @param warehouseId 仓库ID
	 * @return WarehouseDisplayVO 仓库展示信息
	 */
	public WarehouseDisplayVO getDisplay(Long warehouseId) {
		if (warehouseId == null) {
			return null;
		}
		Warehouse warehouse = this.getById(warehouseId);
		return warehouse != null ? buildWarehouseDisplayVO(warehouse) : null;
	}

	/**
	 * 根据仓库ID获取名称
	 * @param warehouseId 仓库ID
	 * @return 仓库名称
	 */
	public String getNameById(Long warehouseId) {
		if (warehouseId == null) {
			return null;
		}
		Warehouse warehouse = this.getById(warehouseId);
		return warehouse != null ? warehouse.getWarehouseName() : null;
	}

	/**
	 * 批量根据仓库ID获取名称映射
	 * @param warehouseIds 仓库ID集合
	 * @return 仓库ID与名称的映射
	 */
	public Map<Long, String> getNameMapByIds(Collection<Long> warehouseIds) {
		if (CollectionUtils.isEmpty(warehouseIds)) {
			return Collections.emptyMap();
		}

		Set<Long> validIds = warehouseIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (validIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Warehouse> warehouses = this.listByIds(validIds);
		return warehouses.stream()
				.collect(Collectors.toMap(Warehouse::getId, Warehouse::getWarehouseName, (v1, v2) -> v1));
	}

	/**
	 * 查询自有仓库列表
	 * <p>
	 * 用于库存预测，只返回自有仓库
	 *
	 * @param warehouseId 可选，指定仓库ID进一步筛选
	 * @return 自有仓库列表
	 */
	public List<Warehouse> listOwnWarehouses(Long warehouseId) {
		List<Warehouse> list = baseMapper.selectOwnWarehouses(warehouseId);
		Set<Long> scope = ownWarehouseScope();
		if (scope == null) {
			return list;
		}
		return list.stream().filter(w -> scope.contains(w.getId())).collect(Collectors.toList());
	}

	/**
	 * 查询区域内自有仓库列表
	 */
	public List<Warehouse> listOwnByRegion(Long regionId) {
		return baseMapper.selectOwnByRegion(regionId);
	}

	/**
	 * 按区域 ID 集合查询自有仓库列表
	 */
	public List<Warehouse> listOwnByRegionIds(Collection<Long> regionIds) {
		if (CollectionUtils.isEmpty(regionIds)) {
			return Collections.emptyList();
		}
		return baseMapper.selectOwnByRegionIds(regionIds);
	}

	/**
	 * 根据区域ID查询仓库ID列表
	 * @param regionId 区域ID
	 * @return 仓库ID列表
	 */
	public List<Long> getIdsByRegionId(Long regionId) {
		if (regionId == null) {
			return Collections.emptyList();
		}
		List<Warehouse> warehouses = baseMapper.selectByRegionIds(Collections.singletonList(regionId));
		return warehouses.stream()
				.filter(w -> w.getStatus() != null && w.getStatus() == 1)
				.map(Warehouse::getId)
				.collect(Collectors.toList());
	}

	// ==================== FBO 仓库管理方法 ====================

	/**
	 * 根据平台和平台仓库ID查询仓库
	 * @param platform 平台
	 * @param platformWarehouseId 平台仓库ID
	 * @return 仓库实体，不存在返回 null
	 */
	public Warehouse getByPlatformWarehouseId(String platform, String platformWarehouseId) {
		return baseMapper.selectByPlatformWarehouseId(platform, platformWarehouseId);
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
	public Warehouse getByPlatformShopAndWarehouseId(String platform, Long shopId, String platformWarehouseId) {
		return baseMapper.selectByPlatformShopAndWarehouseId(platform, shopId, platformWarehouseId);
	}

	/**
	 * 查询平台下已存在的平台仓库ID集合
	 * @param platform 平台
	 * @return 已存在的平台仓库ID集合
	 */
	public Set<String> getExistingPlatformWarehouseIds(String platform) {
		return baseMapper.selectExistingPlatformWarehouseIds(platform);
	}

	/**
	 * 生成 FBO 仓库编码
	 * @param platform 平台
	 * @return 仓库编码，格式：FBO-{PLATFORM}-{序号}
	 */
	public String generateFboWarehouseCode(String platform) {
		String prefix = "FBO-" + platform.toUpperCase() + "-";

		// 查询当前平台最大序号
		Warehouse lastWarehouse = baseMapper.selectMaxCodeByPrefix(prefix);

		int nextSeq = 1;
		if (lastWarehouse != null) {
			String lastCode = lastWarehouse.getWarehouseCode();
			String seqStr = lastCode.substring(prefix.length());
			try {
				nextSeq = Integer.parseInt(seqStr) + 1;
			} catch (NumberFormatException e) {
				// 忽略解析错误，使用默认值
			}
		}

		return String.format("%s%03d", prefix, nextSeq);
	}

	/**
	 * 创建 FBO 仓库
	 * <p>
	 * 使用乐观重试机制处理并发编码冲突：当多个请求同时创建仓库时，
	 * 可能生成相同的编码导致唯一约束冲突，此时自动重试生成新编码。
	 *
	 * @param platform 平台
	 * @param shopId 店铺ID
	 * @param platformWarehouseId 平台仓库ID（Ozon 为 cluster_id）
	 * @param warehouseName 仓库名称
	 * @param regionId 区域ID（由调用方通过 PlatformRegionMappingService 查询后传入）
	 * @return 创建的仓库实体
	 */
	public Warehouse createFboWarehouse(String platform, Long shopId, String platformWarehouseId,
			String warehouseName, Long regionId) {
		// 检查是否已存在
		Warehouse existing = getByPlatformShopAndWarehouseId(platform, shopId, platformWarehouseId);
		if (existing != null) {
			log.info("FBO 仓库已存在，跳过创建: platform={}, shopId={}, platformWarehouseId={}",
					platform, shopId, platformWarehouseId);
			return existing;
		}

		Assert.notNull(regionId, "未找到平台 " + platform + " 对应的区域映射，请务必先在区域管理中配置区域与平台的映射关系，否则无法自动同步FBO库存并创建仓库");

		// 乐观重试机制：处理并发编码冲突
		int maxRetries = 3;
		for (int attempt = 0; attempt < maxRetries; attempt++) {
			try {
				Warehouse warehouse = new Warehouse();
				warehouse.setWarehouseCode(generateFboWarehouseCode(platform));
				warehouse.setWarehouseName(warehouseName);
				warehouse.setWarehouseType(WarehouseTypeEnum.FBO.getCode());
				warehouse.setRegionId(regionId);
				warehouse.setPlatform(platform);
				warehouse.setShopId(shopId);
				warehouse.setPlatformWarehouseId(platformWarehouseId);
				warehouse.setStatus(1); // 默认启用

				this.save(warehouse);
				log.info("FBO 仓库创建成功: warehouseCode={}, warehouseName={}, shopId={}, regionId={}",
						warehouse.getWarehouseCode(), warehouse.getWarehouseName(), shopId, regionId);
				return warehouse;
			} catch (org.springframework.dao.DuplicateKeyException e) {
				// 可能是编码冲突，也可能是平台仓库ID重复
				// 再次检查是否已存在（可能被并发请求创建）
				existing = getByPlatformShopAndWarehouseId(platform, shopId, platformWarehouseId);
				if (existing != null) {
					log.info("FBO 仓库已被并发创建: platform={}, shopId={}, platformWarehouseId={}",
							platform, shopId, platformWarehouseId);
					return existing;
				}

				if (attempt == maxRetries - 1) {
					log.error("FBO 仓库创建失败，编码冲突重试次数已耗尽: platform={}, shopId={}, platformWarehouseId={}",
							platform, shopId, platformWarehouseId);
					throw new IllegalStateException("仓库编码生成冲突，请重试", e);
				}
				log.warn("FBO 仓库编码冲突，重试第 {} 次: platform={}", attempt + 1, platform);
			}
		}

		// 理论上不会到达这里
		throw new IllegalStateException("仓库创建失败");
	}

	/**
	 * 校验仓库属于指定区域内的 OWN 仓库
	 * @param warehouseId 仓库ID
	 * @param regionId 区域ID
	 */
	public void validateOwnWarehouseInRegion(Long warehouseId, Long regionId) {
		Warehouse warehouse = this.getById(warehouseId);
		Assert.notNull(warehouse, "仓库不存在");
		Assert.isTrue(WarehouseTypeEnum.OWN.getCode().equals(warehouse.getWarehouseType()),
				"入库仓库必须是自有仓(OWN)类型");
		Assert.isTrue(Objects.equals(warehouse.getRegionId(), regionId),
				"入库仓库必须在物流单目标区域内");
	}

	/**
	 * 获取或创建 FBO 仓库（按店铺 + Cluster）
	 * <p>
	 * 根据平台、店铺和 Cluster 查找仓库，不存在则自动创建。
	 * 用于 Ozon FBO 库存同步时发现新仓库的场景。
	 *
	 * @param platform 平台
	 * @param shopId 店铺ID
	 * @param clusterId Cluster ID（作为 platformWarehouseId）
	 * @param clusterName Cluster 名称（作为仓库名）
	 * @param regionId 区域ID（由调用方通过 PlatformRegionMappingService 查询后传入）
	 * @return 仓库实体
	 */
	public Warehouse getOrCreateFboWarehouse(String platform, Long shopId, String clusterId,
			String clusterName, Long regionId) {
		// 使用 (platform, shopId, clusterId) 进行查找
		Warehouse existing = getByPlatformShopAndWarehouseId(platform, shopId, clusterId);
		if (existing != null) {
			return existing;
		}

		// 不存在则创建
		return createFboWarehouse(platform, shopId, clusterId, clusterName, regionId);
	}

}