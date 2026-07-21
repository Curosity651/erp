<template>
  <div class="stocktake-progress-wrapper">
    <a-row :gutter="16">
      <!-- Card 1: 总览 (Total) -->
      <a-col :span="6">
        <div class="metric-card">
          <div class="card-icon-wrapper total-bg">
            <appstore-outlined />
          </div>
          <div class="card-info">
            <div class="card-label">总盘点数</div>
            <div class="card-value-primary">{{ progress.totalCount }}</div>
          </div>
        </div>
      </a-col>

      <!-- Card 2: 进度 (Progress) -->
      <a-col :span="6">
        <div class="metric-card">
          <div class="card-chart-wrapper">
            <a-progress
              type="circle"
              :percent="progress.progressPercent"
              :size="40"
              :stroke-width="10"
              :show-info="false"
            />
            <span class="chart-text">{{ progress.progressPercent }}%</span>
          </div>
          <div class="card-info">
            <div class="card-label">已盘 / 总数</div>
            <div class="card-value-primary">
              <span class="text-success">{{ progress.countedCount }}</span>
              <span class="separator">/</span>
              <span>{{ progress.totalCount }}</span>
            </div>
          </div>
        </div>
      </a-col>

      <!-- Card 3: 盘盈 (Profit) -->
      <a-col :span="6">
        <div class="metric-card">
          <div class="card-icon-wrapper profit-bg">
            <arrow-up-outlined />
          </div>
          <div class="card-info">
            <div class="card-label">盘盈 SKU</div>
            <div class="card-value-primary text-profit">
              {{ progress.profitCount }}
              <span class="unit">项</span>
            </div>
          </div>
          <div class="card-secondary-info">
            <div class="secondary-label">差异量</div>
            <div class="secondary-value text-profit">+{{ progress.profitQuantity }}</div>
          </div>
        </div>
      </a-col>

      <!-- Card 4: 盘亏 (Loss) -->
      <a-col :span="6">
        <div class="metric-card">
          <div class="card-icon-wrapper loss-bg">
            <arrow-down-outlined />
          </div>
          <div class="card-info">
            <div class="card-label">盘亏 SKU</div>
            <div class="card-value-primary text-loss">
              {{ progress.lossCount }}
              <span class="unit">项</span>
            </div>
          </div>
          <div class="card-secondary-info">
            <div class="secondary-label">差异量</div>
            <div class="secondary-value text-loss">-{{ Math.abs(progress.lossQuantity) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { AppstoreOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons-vue'
import type { StocktakeProgressVO } from '@/api/wms/stocktake/types'

defineOptions({ name: 'StocktakeProgress' })

defineProps<{
  progress: StocktakeProgressVO
  loading?: boolean
}>()
</script>

<style scoped lang="less">
.stocktake-progress-wrapper {
  margin-bottom: 12px;
}

.metric-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    border-color: #e6f7ff;
  }
}

/* Icon Wrapper */
.card-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;

  &.total-bg {
    background: #e6f7ff;
    color: #1890ff;
  }

  &.profit-bg {
    background: #f6ffed;
    color: #52c41a;
  }

  &.loss-bg {
    background: #fff1f0;
    color: #ff4d4f;
  }
}

/* Chart Wrapper */
.card-chart-wrapper {
  position: relative;
  margin-right: 12px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;

  .chart-text {
    position: absolute;
    font-size: 10px;
    font-weight: 600;
    color: #595959;
  }
}

/* Main Info Section (Left/Center) */
.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 2px;
}

.card-value-primary {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  line-height: 1.1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;

  .unit {
    font-size: 12px;
    font-weight: normal;
    color: #8c8c8c;
    margin-left: 2px;
  }

  .separator {
    margin: 0 2px;
    color: #bfbfbf;
    font-size: 16px;
  }
}

/* Secondary Info Section (Right) - for Profit/Loss details */
.card-secondary-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid rgba(0, 0, 0, 0.06);
}

.secondary-label {
  font-size: 10px;
  color: #8c8c8c;
  margin-bottom: 2px;
}

.secondary-value {
  font-size: 14px;
  font-weight: 500;
}

/* Text Colors */
.text-profit {
  color: #52c41a;
}
.text-loss {
  color: #ff4d4f;
}
.text-success {
  color: #1890ff;
}
</style>
