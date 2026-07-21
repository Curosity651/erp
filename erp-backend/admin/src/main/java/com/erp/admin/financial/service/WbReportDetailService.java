package com.erp.admin.financial.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.financial.mapper.WbReportDetailMapper;
import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.financial.model.qo.WbReportDetailQO;
import com.erp.admin.financial.model.vo.WbReportDetailPageVO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.util.SpringUtils;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WbReportDetailService extends ExtendServiceImpl<WbReportDetailMapper, WbReportDetail> {

    /**
     * 批量保存或更新财务报表数据
     * <p>
     * 使用 MyBatis-Plus 的 saveBatch 方法进行批量插入
     * 如果遇到唯一键冲突（period_type + rrd_id），会自动更新记录
     * <p>
     * 批次大小：1000 条记录
     * <p>
     * 注意：使用独立事务，避免长时间持有锁
     *
     * @param periodType 报表周期类型（weekly/daily）
     * @param details    财务报表明细列表
     * @return 实际插入/更新的记录数
     */
    public int batchSaveOrUpdate(String periodType, List<WbReportDetail> details) {
        if (periodType == null || periodType.isEmpty() || details == null || details.isEmpty()) {
            return 0;
        }

        // 过滤掉没有 rrdId 和 periodType 的记录
        List<WbReportDetail> validDetails = details.stream()
                .filter(d -> d.getRrdId() != null && d.getPeriodType() != null)
                .collect(Collectors.toList());

        if (validDetails.isEmpty()) {
            return 0;
        }

        int batchSize = 1000;
        int total = 0;

		WbReportDetailService proxy = SpringUtils.getBean(WbReportDetailService.class);

		for (int i = 0; i < validDetails.size(); i += batchSize) {
            int end = Math.min(i + batchSize, validDetails.size());
            List<WbReportDetail> subList = validDetails.subList(i, end);
			total += proxy.saveOrUpdateBatchByPeriodTypeAndRrdId(periodType, subList);
        }

        return total;
    }

    /**
     * 按 periodType + rrdId 分批保存或更新（单批，无递归批处理）
     *
     * @param periodType 报表周期类型（weekly/daily）
     * @param batch      待保存的批次数据
     * @return 受影响的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateBatchByPeriodTypeAndRrdId(String periodType, List<WbReportDetail> batch) {
        if (periodType == null || periodType.isEmpty() || batch == null || batch.isEmpty()) {
            return 0;
        }

        // 当前批次的 rrdId 集合
        Set<Long> rrdIds = batch.stream()
                .map(WbReportDetail::getRrdId)
                .collect(Collectors.toSet());

        List<WbReportDetail> existed = this.baseMapper.selectByPeriodTypeAndRrdIds(periodType, rrdIds);

        Map<Long, Long> rrdIdToId = existed.stream()
                .filter(e -> e.getRrdId() != null && e.getId() != null)
                .collect(Collectors.toMap(WbReportDetail::getRrdId, WbReportDetail::getId, (a, b) -> a));

        List<WbReportDetail> toInsert = new ArrayList<>();
        List<WbReportDetail> toUpdate = new ArrayList<>();

        for (WbReportDetail detail : batch) {
            Long id = rrdIdToId.get(detail.getRrdId());
            if (id == null) {
                toInsert.add(detail);
            }
            else {
                detail.setId(id);
				detail.setCreateTime(null);
				toUpdate.add(detail);
            }
        }

        int affected = 0;
        if (!toInsert.isEmpty()) {
			this.saveBatch(toInsert);
            affected += toInsert.size();
        }
        if (!toUpdate.isEmpty()) {
			for (WbReportDetail wbReportDetail : toUpdate) {
				affected += this.baseMapper.updateById(wbReportDetail);
			}
        }

        return affected;
    }

    public PageResult<WbReportDetailPageVO> queryPage(PageParam pageParam, WbReportDetailQO qo) {
        return this.baseMapper.queryPage(pageParam, qo);
    }

    public List<WbReportDetail> listForExport(WbReportDetailQO qo) {
		int limit = 100000;
		return this.baseMapper.selectListByQo(qo, limit);
    }


}
