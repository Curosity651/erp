package com.erp.admin.order.mapper;

import com.erp.admin.order.model.entity.ErpLabelBatchFile;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.toolkit.WrappersX;

public interface ErpLabelBatchFileMapper extends ExtendMapper<ErpLabelBatchFile> {
    default java.util.List<ErpLabelBatchFile> selectListByBatchId(Long batchId) {
        LambdaQueryWrapperX<ErpLabelBatchFile> wrapper = WrappersX.lambdaQueryX(ErpLabelBatchFile.class)
            .eq(ErpLabelBatchFile::getBatchId, batchId);
        return this.selectList(wrapper);
    }

    default boolean existsByBatchIdAndFileName(Long batchId, String fileName) {
        LambdaQueryWrapperX<ErpLabelBatchFile> wrapper = WrappersX.lambdaQueryX(ErpLabelBatchFile.class)
            .eq(ErpLabelBatchFile::getBatchId, batchId)
            .eq(ErpLabelBatchFile::getFileName, fileName)
            .last("limit 1");
        return this.selectCount(wrapper) > 0;
    }
}


