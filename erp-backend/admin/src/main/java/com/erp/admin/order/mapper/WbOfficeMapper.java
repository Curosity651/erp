package com.erp.admin.order.mapper;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.admin.order.model.entity.WbOffice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.toolkit.WrappersX;

@Mapper
public interface WbOfficeMapper extends BaseMapper<WbOffice> {
	@Select({
			"<script>",
			"SELECT * FROM (",
			"    SELECT *, ",
			"           ROW_NUMBER() OVER (PARTITION BY office_id ORDER BY id) as rn ",
			"    FROM wb_office ",
			"    WHERE office_id IN ",
			"    <foreach item='oid' collection='officeIds' open='(' separator=',' close=')'>",
			"        #{oid}",
			"    </foreach>",
			") t WHERE rn = 1",
			"</script>"
	})
	List<WbOffice> selectByOfficeIds(@Param("officeIds") Collection<Long> officeIds);

	/**
	 * 根据店铺ID和仓库ID查询
	 */
	default WbOffice selectByShopIdAndOfficeId(Long shopId, Long officeId) {
		return selectOne(WrappersX.lambdaQueryX(WbOffice.class)
				.eq(WbOffice::getShopId, shopId)
				.eq(WbOffice::getOfficeId, officeId));
	}
}