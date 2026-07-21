package com.erp.admin.product.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.product.mapper.SupplierMapper;
import com.erp.admin.product.model.entity.Supplier;
import com.erp.admin.product.model.qo.SupplierQO;
import com.erp.admin.product.model.vo.SupplierPageVO;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.service.OssService;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 供应商
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Service
@RequiredArgsConstructor
public class SupplierService extends ExtendServiceImpl<SupplierMapper, Supplier> {

	private final OssService ossService;

	/**
	 * 根据QueryObeject查询分页数据
	 * @param pageParam 分页参数
	 * @param qo 查询参数对象
	 * @return PageResult<SupplierPageVO> 分页数据
	 */
	public PageResult<SupplierPageVO> queryPage(PageParam pageParam, SupplierQO qo) {
		return baseMapper.queryPage(pageParam, qo);
	}

	/**
	 * 获取供应商选项列表（用于下拉选择）
	 * @param keyword 搜索关键词（可选）
	 * @return List<SupplierPageVO> 供应商选项列表
	 */
	public List<SupplierPageVO> getSupplierOptions(String keyword) {
		SupplierQO qo = new SupplierQO();
		qo.setStatus(1); // 只查询启用状态的供应商

		// 如果有关键词，设置搜索条件
		if (StringUtils.hasText(keyword)) {
			qo.setSupplierCode(keyword);
			qo.setName(keyword);
		}

		return baseMapper.getSupplierOptions(qo);
	}

	/**
	 * 根据ID获取供应商名称
	 * @param supplierId 供应商ID
	 * @return 供应商名称
	 */
	public String getNameById(Long supplierId) {
		if (supplierId == null) {
			return null;
		}
		Supplier supplier = this.getById(supplierId);
		return supplier != null ? supplier.getName() : null;
	}

	/**
	 * 批量根据ID获取供应商名称映射
	 * @param supplierIds 供应商ID集合
	 * @return Map<supplierId, supplierName>
	 */
	public Map<Long, String> getNameMapByIds(Collection<Long> supplierIds) {
		if (CollectionUtils.isEmpty(supplierIds)) {
			return Collections.emptyMap();
		}
		List<Supplier> suppliers = this.listByIds(supplierIds);
		return suppliers.stream()
				.collect(Collectors.toMap(Supplier::getId, Supplier::getName, (v1, v2) -> v1));
	}

	/** Generate a short-lived preview URL for a supplier in the current tenant. */
	public String getBusinessLicenseUrl(Long supplierId) {
		if (supplierId == null) {
			return null;
		}
		Supplier supplier = this.getById(supplierId);
		if (supplier == null || !StringUtils.hasText(supplier.getBusinessLicensePhoto())) {
			return null;
		}
		return ossService.getUrl(OssBucketKeys.PUBLIC_FILES, supplier.getBusinessLicensePhoto());
	}

}
