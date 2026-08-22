package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.qo.PackShipQO;
import com.erp.admin.wms.model.qo.PackShipPackageQO;
import com.erp.admin.wms.model.vo.LogisticsChannelVO;
import com.erp.admin.wms.model.vo.PackShipOrderVO;
import com.erp.admin.wms.model.vo.PackShipPackagePageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打包签出读模型 Mapper。平台看全部货主，仅当过滤非空时收窄。
 *
 * @author erp
 */
public interface OutboundShippingMapper {

    IPage<PackShipOrderVO> pageOrders(IPage<PackShipOrderVO> page, @Param("qo") PackShipQO qo);

    IPage<PackShipPackagePageVO> pagePackages(IPage<PackShipPackagePageVO> page,
            @Param("qo") PackShipPackageQO qo);

    PackShipPackagePageVO locatePackageByPlatformOrderId(@Param("scanCode") String scanCode);

    PackShipPackagePageVO locatePackageById(@Param("packageId") Long packageId);

    PackShipOrderVO selectOrderById(@Param("id") Long id);

    /** 物流渠道选项（启用的物流商） */
    List<LogisticsChannelVO> selectChannels();

}
