<template>
  <div v-loading="loading" class="payment-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>支付详情</span>
      </template>
    </el-page-header>

    <template v-if="record">
      <el-card class="detail-card" shadow="never">
        <div class="status-row">
          <el-tag :type="statusTagType" size="large">{{ statusLabel }}</el-tag>
          <span v-if="record.closeReason" class="close-reason">
            关闭原因：{{ closeReasonLabel(record.closeReason) }}
          </span>
        </div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="支付ID">{{ record.paymentId }}</el-descriptions-item>
          <el-descriptions-item label="金额">
            <span class="amount">¥{{ record.amount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="交易类型">
            <el-tag size="small">{{ businessTypeLabel(record.businessType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ record.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="状态变更时间">{{ record.statusChangedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="record.expireAt" label="支付截止时间" :span="2">
            {{ record.expireAt }}
          </el-descriptions-item>
        </el-descriptions>

      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPaymentRecord } from '@/api/payment'

const route = useRoute()
const loading = ref(true)
const record = ref(null)

const statusLabelMap = {
  WAITING_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
  SETTLED: '已结算'
}

const closeReasonMap = {
  USER_CANCELLED: '用户取消',
  PAYMENT_EXPIRED: '支付超时',
  ACCEPT_DEADLINE_EXPIRED: '接单截止超时',
  SYSTEM_CANCELLED: '系统取消'
}

const businessTypeMap = {
  PICKUP_REQUEST: '代取服务'
}

const statusLabel = computed(() => statusLabelMap[record.value?.status] || record.value?.status)

const statusTagType = computed(() => {
  const map = {
    WAITING_PAY: 'warning',
    PAID: 'success',
    CLOSED: 'info',
    REFUNDED: 'warning',
    SETTLED: 'success'
  }
  return map[record.value?.status] || 'info'
})

function closeReasonLabel(reason) {
  return closeReasonMap[reason] || reason
}

function businessTypeLabel(type) {
  return businessTypeMap[type] || type
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getPaymentRecord(route.params.id)
    record.value = res
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.payment-detail {
  max-width: 700px;
  margin: 0 auto;
}

.detail-card {
  margin-top: 20px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.close-reason {
  color: #f56c6c;
  font-size: 14px;
}

.detail-info {
  margin-bottom: 24px;
}

.amount {
  font-size: 18px;
  font-weight: bold;
  color: #e6a23c;
}

</style>
