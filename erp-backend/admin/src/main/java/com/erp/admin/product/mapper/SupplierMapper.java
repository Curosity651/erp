package com.erp.admin.product.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.converter.SupplierConverter;
import com.erp.admin.product.model.entity.Supplier;
import com.erp.admin.product.model.qo.SupplierQO;
import com.erp.admin.product.model.vo.SupplierPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 供应商
 *
 * @author ballcat 2025-07-26 22:36:14
 */
public interface SupplierMapper extends ExtendMapper<Supplier> {

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param qo 查询参数
	 * @return PageResult<SupplierPageVO> VO分页数据
	 */
	default PageResult<SupplierPageVO> queryPage(PageParam pageParam, SupplierQO qo) {
		IPage<Supplier> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<Supplier> wrapper = WrappersX.lambdaQueryX(Supplier.class)
			.likeIfPresent(Supplier::getSupplierCode, qo.getSupplierCode())
			.likeIfPresent(Supplier::getName, qo.getName())
			.likeIfPresent(Supplier::getCity, qo.getCity())
			.eqIfPresent(Supplier::getStatus, qo.getStatus())
			.likeIfPresent(Supplier::getBusinessContactName, qo.getBusinessContactName())
			.eqIfPresent(Supplier::getBusinessContactPhone, qo.getBusinessContactPhone())
			.orderByDesc(Supplier::getId);
		this.selectPage(page, wrapper);
		IPage<SupplierPageVO> voPage = page.convert(SupplierConverter.INSTANCE::poToPageVo);
		return new PageResult<>(voPage.getRecords(), voPage.getTotal());
	}

	/**
	 * 获取供应商选项列表（用于下拉选择）
	 * @param qo 查询参数
	 * @return List<SupplierPageVO> 供应商选项列表
	 */
	default List<SupplierPageVO> getSupplierOptions(SupplierQO qo) {
		LambdaQueryWrapperX<Supplier> wrapper = WrappersX.lambdaQueryX(Supplier.class)
			.eqIfPresent(Supplier::getStatus, qo.getStatus())
			.and(qo.getSupplierCode() != null || qo.getName() != null, w -> {
				if (qo.getSupplierCode() != null) {
					w.like(Supplier::getSupplierCode, qo.getSupplierCode());
				}
				if (qo.getName() != null) {
					w.or().like(Supplier::getName, qo.getName());
				}
			})
			.orderByAsc(Supplier::getSupplierCode);

		List<Supplier> suppliers = this.selectList(wrapper);
		return suppliers.stream().map(SupplierConverter.INSTANCE::poToPageVo).collect(Collectors.toList());
	}

}
