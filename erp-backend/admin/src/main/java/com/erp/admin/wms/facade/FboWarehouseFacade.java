package com.erp.admin.wms.facade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.ozon.OzonClient;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.request.warehouse.OzonFboWarehouseListRequest;
import com.erp.admin.platform.ozon.model.response.warehouse.OzonClusterListResponse;
import com.erp.admin.platform.ozon.model.response.warehouse.OzonFboWarehouse;
import com.erp.admin.platform.ozon.model.response.warehouse.OzonFboWarehouseListResponse;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.erp.admin.wms.model.vo.FboWarehouseVO;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * FBO 仓库管理 Facade
 * <p>
 * 负责协调 OzonClient 和 WarehouseService 完成 FBO 仓库的拉取和导入。
 * @deprecated FBO 直接拉取仓库会导致拉取到很多无效仓库
 */
@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class FboWarehouseFacade {

	private static final String CLUSTER_TYPE_OZON = "CLUSTER_TYPE_OZON";
	private static final String CLUSTER_TYPE_CIS = "CLUSTER_TYPE_CIS";
	private static final int MIN_SEARCH_LENGTH = 4;
	private static final List<String> SUPPLY_TYPES = Arrays.asList("CREATE_TYPE_CROSSDOCK", "CREATE_TYPE_DIRECT");

	private final OzonClient ozonClient;
	private final WarehouseService warehouseService;
	private final ShopService shopService;
	private final CredentialService credentialService;

	/**
	 * 从平台拉取 FBO 仓库列表
	 *
	 * @param platform 平台: ozon
	 * @return FBO 仓库列表（标记是否已存在）
	 */
	public List<FboWarehouseVO> fetchFboWarehouses(String platform) {
		Assert.hasText(platform, "平台不能为空");

		// 校验平台是否支持
		PlatformEnum platformEnum = PlatformEnum.fromCode(platform);
		if (platformEnum == null) {
			throw new IllegalArgumentException("不支持的平台: " + platform);
		}

		// 目前只支持 Ozon
		if (platformEnum != PlatformEnum.Ozon) {
			throw new IllegalArgumentException("暂不支持该平台的 FBO 仓库同步: " + platform);
		}

		// 获取启用的 Ozon 店铺（取第一个）
		Shop shop = shopService.getFirstEnabledShopByPlatform(platform);
		if (shop == null) {
			throw new IllegalArgumentException("未找到启用的 " + platform + " 店铺，请先配置店铺");
		}

		// 解析凭证（静态工具类方法）
		OzonCredential credential = credentialService.parseCredential(shop);

		// 调用 Ozon API 获取 FBO 仓库列表
		List<OzonFboWarehouse> ozonWarehouses;
		try {
			ozonWarehouses = fetchOzonFboWarehouses(credential);
		} catch (PlatformApiException e) {
			log.error("获取 Ozon FBO 仓库列表失败: {}", e.getMessage());
			throw new IllegalArgumentException("获取平台仓库列表失败: " + e.getMessage());
		}

		// 查询已存在的平台仓库ID
		Set<String> existingIds = warehouseService.getExistingPlatformWarehouseIds(platform);

		// 转换为 VO
		List<FboWarehouseVO> result = new ArrayList<>();
		for (OzonFboWarehouse ow : ozonWarehouses) {
			FboWarehouseVO vo = new FboWarehouseVO();
			vo.setPlatformWarehouseId(String.valueOf(ow.getWarehouseId()));
			vo.setWarehouseName(ow.getName());
			vo.setExists(existingIds.contains(String.valueOf(ow.getWarehouseId())));
			result.add(vo);
		}

		return result;
	}

	/**
	 * 从 Ozon 平台获取 FBO 仓库列表（包含地址信息）
	 * <p>
	 * 实现方式：
	 * 1. 调用 /v1/cluster/list 获取所有仓库的 ID 和名称（OZON + CIS）
	 * 2. 对于名称长度 >= 4 的仓库，调用 /v1/warehouse/fbo/list 获取地址信息
	 * 3. 合并数据返回完整的仓库列表
	 *
	 * @param credential Ozon 凭证
	 * @return FBO 仓库列表
	 */
	private List<OzonFboWarehouse> fetchOzonFboWarehouses(OzonCredential credential) {
		// 1. 获取所有集群仓库（OZON + CIS）
		List<OzonClusterListResponse.ClusterWarehouse> clusterWarehouses = new ArrayList<>();
		clusterWarehouses.addAll(extractWarehousesFromCluster(
				ozonClient.getClusterList(credential, CLUSTER_TYPE_OZON)));
		clusterWarehouses.addAll(extractWarehousesFromCluster(
				ozonClient.getClusterList(credential, CLUSTER_TYPE_CIS)));

		if (clusterWarehouses.isEmpty()) {
			log.warn("[FBO] 未获取到任何集群仓库");
			return new ArrayList<>();
		}

		log.info("[FBO] 从集群获取到 {} 个仓库", clusterWarehouses.size());

		// 2. 构建仓库 ID -> 基本信息的映射
		Map<Long, OzonFboWarehouse> warehouseMap = new HashMap<>();
		for (OzonClusterListResponse.ClusterWarehouse cw : clusterWarehouses) {
			OzonFboWarehouse warehouse = new OzonFboWarehouse();
			warehouse.setWarehouseId(cw.getWarehouseId());
			warehouse.setName(cw.getName());
			warehouse.setWarehouseType(cw.getType());
			warehouseMap.put(cw.getWarehouseId(), warehouse);
		}

		// 3. 对于名称长度 >= 4 的仓库，查询地址信息
		for (OzonClusterListResponse.ClusterWarehouse cw : clusterWarehouses) {
			String name = cw.getName();
			if (name != null && name.length() >= MIN_SEARCH_LENGTH) {
				try {
					OzonFboWarehouseListRequest request = OzonFboWarehouseListRequest.builder()
							.filterBySupplyType(SUPPLY_TYPES)
							.search(name)
							.build();
					OzonFboWarehouseListResponse response = ozonClient.searchFboWarehouses(credential, request);

					// 根据仓库 ID 匹配，填充地址
					if (response.getSearch() != null) {
						for (OzonFboWarehouse found : response.getSearch()) {
							if (warehouseMap.containsKey(found.getWarehouseId())) {
								warehouseMap.get(found.getWarehouseId()).setAddress(found.getAddress());
							}
						}
					}
				} catch (Exception e) {
					// 单个仓库查询失败不影响整体，记录日志继续
					log.warn("[FBO] 查询仓库地址失败: name={}, error={}", name, e.getMessage());
				}
			}
		}

		log.info("[FBO] 获取 FBO 仓库列表完成，共 {} 个", warehouseMap.size());
		return new ArrayList<>(warehouseMap.values());
	}

	/**
	 * 从集群响应中提取仓库列表
	 */
	private List<OzonClusterListResponse.ClusterWarehouse> extractWarehousesFromCluster(
			OzonClusterListResponse response) {
		List<OzonClusterListResponse.ClusterWarehouse> result = new ArrayList<>();
		if (response == null || response.getClusters() == null) {
			return result;
		}

		for (OzonClusterListResponse.Cluster cluster : response.getClusters()) {
			if (cluster.getLogisticClusters() != null) {
				for (OzonClusterListResponse.LogisticCluster lc : cluster.getLogisticClusters()) {
					if (lc.getWarehouses() != null) {
						result.addAll(lc.getWarehouses());
					}
				}
			}
		}
		return result;
	}

}
