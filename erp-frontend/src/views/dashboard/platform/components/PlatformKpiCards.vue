<template>
  <a-row :gutter="[24, 24]">
    <!-- 在库总量 -->
    <a-col :xs="24" :sm="12" :lg="6">
      <div class="kpi-tile tile-blue">
        <div class="kpi-head">
          <DatabaseOutlined class="kpi-icon" />
          <span class="kpi-title">{{ t('platform.dashboard.kpi.onHand') }}</span>
        </div>
        <div class="kpi-main">
          {{ formatQty(data?.onHandQty) }}<span class="kpi-unit">{{ t('platform.dashboard.unit.pieces') }}</span>
        </div>
        <div class="kpi-sub">
          <span>{{ t('platform.dashboard.kpi.skuCount', { count: formatQty(data?.skuCount) }) }}</span>
          <span class="dot">·</span>
          <span>{{ t('platform.dashboard.kpi.ownerCount', { count: formatQty(data?.ownerCount) }) }}</span>
        </div>
      </div>
    </a-col>

    <!-- 今日入库 -->
    <a-col :xs="24" :sm="12" :lg="6">
      <div class="kpi-tile tile-green">
        <div class="kpi-head">
          <DownloadOutlined class="kpi-icon" />
          <span class="kpi-title">{{ t('platform.dashboard.kpi.todayInbound') }}</span>
        </div>
        <div class="kpi-main">
          {{ formatQty(data?.todayInboundQty) }}<span class="kpi-unit">{{ t('platform.dashboard.unit.pieces') }}</span>
        </div>
        <div class="kpi-sub">
          <span>{{ t('platform.dashboard.kpi.orderCount', { count: formatQty(data?.todayInboundOrders) }) }}</span>
        </div>
      </div>
    </a-col>

    <!-- 今日出库 -->
    <a-col :xs="24" :sm="12" :lg="6">
      <div class="kpi-tile tile-cyan">
        <div class="kpi-head">
          <UploadOutlined class="kpi-icon" />
          <span class="kpi-title">{{ t('platform.dashboard.kpi.todayOutbound') }}</span>
        </div>
        <div class="kpi-main">
          {{ formatQty(data?.todayOutboundQty) }}<span class="kpi-unit">{{ t('platform.dashboard.unit.pieces') }}</span>
        </div>
        <div class="kpi-sub">
          <span>{{ t('platform.dashboard.kpi.orderCount', { count: formatQty(data?.todayOutboundOrders) }) }}</span>
        </div>
      </div>
    </a-col>

    <!-- 待处理作业积压 -->
    <a-col :xs="24" :sm="12" :lg="6">
      <div class="kpi-tile tile-orange">
        <div class="kpi-head">
          <FieldTimeOutlined class="kpi-icon" />
          <span class="kpi-title">{{ t('platform.dashboard.kpi.pending') }}</span>
        </div>
        <div class="pending-grid">
          <div class="pending-item">
            <span class="pending-num">{{ data?.pending?.receiving ?? 0 }}</span>
            <span class="pending-label">{{ t('platform.dashboard.kpi.pendingReceiving') }}</span>
          </div>
          <div class="pending-item">
            <span class="pending-num">{{ data?.pending?.putaway ?? 0 }}</span>
            <span class="pending-label">{{ t('platform.dashboard.kpi.pendingPutaway') }}</span>
          </div>
          <div class="pending-item">
            <span class="pending-num">{{ data?.pending?.pickPack ?? 0 }}</span>
            <span class="pending-label">{{ t('platform.dashboard.kpi.pendingPickPack') }}</span>
          </div>
          <div class="pending-item">
            <span class="pending-num">{{ data?.pending?.outbound ?? 0 }}</span>
            <span class="pending-label">{{ t('platform.dashboard.kpi.pendingOutbound') }}</span>
          </div>
        </div>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import {
  DatabaseOutlined,
  DownloadOutlined,
  UploadOutlined,
  FieldTimeOutlined
} from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import type { OpsOverviewVO } from '@/api/platform-dashboard/types'

defineProps<{ data?: OpsOverviewVO }>()
const { t, locale } = useI18n()

function formatQty(v?: number): string {
  if (v == null) return '0'
  return new Intl.NumberFormat(locale.value, {
    notation: v >= 10000 ? 'compact' : 'standard',
    maximumFractionDigits: 1
  }).format(v)
}
</script>

<style scoped>
.kpi-tile {
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  height: 100%;
  min-height: 148px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: all 0.3s ease;
}

.kpi-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
}

.tile-blue {
  background: linear-gradient(135deg, #1890ff 0%, #0958d9 100%);
}
.tile-green {
  background: linear-gradient(135deg, #52c41a 0%, #237804 100%);
}
.tile-cyan {
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);
}
.tile-orange {
  background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);
}

.kpi-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.kpi-icon {
  font-size: 18px;
  opacity: 0.9;
}

.kpi-title {
  font-size: 14px;
  font-weight: 500;
  opacity: 0.95;
}

.kpi-main {
  font-size: clamp(26px, 3vw, 36px);
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.15;
  display: flex;
  align-items: baseline;
}

.kpi-unit {
  font-size: 15px;
  margin-left: 4px;
  opacity: 0.85;
}

.kpi-sub {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 6px;
}

.kpi-sub .dot {
  opacity: 0.7;
}

.pending-grid {
  margin-top: 4px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
  flex: 1;
  align-content: center;
}

.pending-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.pending-num {
  font-size: 22px;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1;
}

.pending-label {
  font-size: 12px;
  opacity: 0.85;
}
</style>
