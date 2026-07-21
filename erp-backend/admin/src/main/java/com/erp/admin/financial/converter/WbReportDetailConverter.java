package com.erp.admin.financial.converter;

import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.financial.model.vo.WbReportDetailExportVO;
import com.erp.admin.financial.model.vo.WbReportDetailPageVO;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * WB 财务报表明细转换器
 */
@Mapper
public interface WbReportDetailConverter {

	WbReportDetailConverter INSTANCE = Mappers.getMapper(WbReportDetailConverter.class);

	WbReportDetailExportVO toExportVO(WbReportDetail detail);

	List<WbReportDetailExportVO> toExportVOList(List<WbReportDetail> details);

	@Mapping(target = "isKgvpV2", expression = "java(detail.getIsKgvpV2() != null ? detail.getIsKgvpV2() == 1 : null)")
	WbReportDetailPageVO toPageVO(WbReportDetail detail);

	List<WbReportDetailPageVO> toPageVOList(List<WbReportDetail> details);

}
