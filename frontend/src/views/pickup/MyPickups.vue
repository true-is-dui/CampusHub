<template>
  <div class="my-pickups">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="我发布的" name="PUBLISHER" />
      <el-tab-pane label="我接单的" name="ACCEPTOR" />
    </el-tabs>

    <div class="filter-bar">
      <el-select
        v-model="statusFilter"
        placeholder="筛选状态"
        clearable
        style="width: 160px"
        @change="loadList"
      >
        <el-option label="全部" value="" />
        <el-option label="待支付" value="WAITING_PAYMENT" />
        <el-option label="待接单" value="WAITING_ACCEPT" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
    </div>

    <div v-loading="loading" class="pickup-list">
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-card
        v-for="item in list"
        :key="item.pickupId"
        class="pickup-card"
        shadow="hover"
        @click="goDetail(item.pickupId)"
      >
        <div class="card-header">
          <span class="card-title">{{ item.campus }} - {{ item.pickupLocation }}</span>
          <div class="header-right-tags">
            <el-tag :type="getTagType(item.status)" size="small">
              {{ getStatusLabel(item.status) }}
            </el-tag>
            <span v-if="item.status === 'WAITING_PAYMENT' && countdowns[item.pickupId]" class="card-countdown">
              {{ countdowns[item.pickupId] }}
            </span>
          </div>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span>送达：{{ item.deliveryLocation }}</span>
          </div>
          <div class="info-row">
            <span v-if="item.rewardType === 'PAID'" class="reward">¥{{ item.rewardAmount }}</span>
            <span v-else class="reward-free">无报酬</span>
            <span class="time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </div>
        <div
          v-if="activeTab === 'PUBLISHER' && (item.status === 'WAITING_PAYMENT' || item.status === 'WAITING_ACCEPT')"
          class="card-actions"
          @click.stop
        >
          <el-button
            v-if="item.status === 'WAITING_PAYMENT'"
            type="primary"
            size="small"
            @click.stop="handlePay(item)"
          >
            继续支付
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click.stop="handleCancel(item)"
          >
            取消订单
          </el-button>
        </div>
      </el-card>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyPickups, cancelPickup, getPaymentEntry } from '@/api/pickup'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('PUBLISHER')
const statusFilter = ref('')
const list = ref([])
const countdowns = reactive({})
let countdownTimer = null

function startCountdowns() {
  stopCountdowns()
  const now = Date.now()
  list.value.forEach(item => {
    if (item.status === 'WAITING_PAYMENT' && item.paymentExpireAt) {
      const expireAt = new Date(item.paymentExpireAt.replace(' ', 'T')).getTime()
      const remaining = Math.max(0, Math.floor((expireAt - now) / 1000))
      if (remaining > 0) {
        const m = String(Math.floor(remaining / 60)).padStart(2, '0')
        const s = String(remaining % 60).padStart(2, '0')
        countdowns[item.pickupId] = `${m}:${s}`
      } else {
        countdowns[item.pickupId] = '已过期'
      }
    }
  })
  countdownTimer = setInterval(() => {
    const now = Date.now()
    list.value.forEach(item => {
      if (item.status === 'WAITING_PAYMENT' && item.paymentExpireAt) {
        const expireAt = new Date(item.paymentExpireAt.replace(' ', 'T')).getTime()
        const remaining = Math.max(0, Math.floor((expireAt - now) / 1000))
        if (remaining > 0) {
          const m = String(Math.floor(remaining / 60)).padStart(2, '0')
          const s = String(remaining % 60).padStart(2, '0')
          countdowns[item.pickupId] = `${m}:${s}`
        } else {
          countdowns[item.pickupId] = '已过期'
        }
      }
    })
  }, 1000)
}

function stopCountdowns() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap = {
  WAITING_PAYMENT: '待支付',
  WAITING_ACCEPT: '待接单',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

function getStatusLabel(status) {
  return statusMap[status] || status
}

function getTagType(status) {
  const map = {
    WAITING_PAYMENT: 'warning',
    WAITING_ACCEPT: 'info',
    IN_PROGRESS: '',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function onTabChange() {
  pagination.page = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      role: activeTab.value,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const res = await getMyPickups(params)
    list.value = res.data?.list || []
    pagination.total = res.data?.total || 0
    startCountdowns()
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/pickup/${id}`)
}

async function handlePay(item) {
  try {
    const res = await getPaymentEntry(item.pickupId)
    const url = res.data?.payEntry || res.data?.payUrl || res.data
    if (url) {
      window.location.href = url
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleCancel(item) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因（可选）', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因',
      type: 'warning'
    })
    await cancelPickup(item.pickupId, { reason: reason || '' })
    ElMessage.success('订单已取消')
    loadList()
  } catch (e) {
    if (e !== 'cancel' && e?.action !== 'cancel') {
      // error handled by interceptor
    }
  }
}

onMounted(() => {
  loadList()
})

onUnmounted(() => {
  stopCountdowns()
})
</script>

<style scoped>
.my-pickups {
  max-width: 800px;
  margin: 0 auto;
}

.filter-bar {
  margin-bottom: 16px;
}

.pickup-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.pickup-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.pickup-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.header-right-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-countdown {
  font-size: 13px;
  font-weight: bold;
  color: #e6a23c;
  font-family: monospace;
}

.card-body {
  font-size: 14px;
  color: #606266;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.reward {
  color: #e6a23c;
  font-weight: 500;
}

.reward-free {
  color: #909399;
}

.time {
  color: #909399;
  font-size: 13px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
