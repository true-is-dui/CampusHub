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
          @change="onFilterChange"
      >
        <el-option label="全部" value="" />
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
          <span class="card-title">{{ campusLabel(item.campus) }} - {{ item.pickupLocation }}</span>
          <el-tag :type="getTagType(item.status)" size="small">
            {{ getStatusLabel(item.status) }}
          </el-tag>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span>送达：{{ item.deliveryLocation }}</span>
          </div>
          <!-- 物品描述预览 -->
          <div v-if="item.itemDescriptionPreview" class="desc-preview">
            {{ item.itemDescriptionPreview }}
          </div>
          <div class="info-row">
            <span v-if="item.rewardType === 'PAID'" class="reward">{{ item.rewardAmount }} 积分</span>
            <span v-else class="reward-free">无报酬</span>
            <span class="time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <!-- 已完成：显示完成时间 -->
          <div v-if="item.status === 'COMPLETED' && item.completedAt" class="info-row">
            <span class="completed-time">完成于 {{ formatTime(item.completedAt) }}</span>
          </div>
          <!-- 已取消：显示取消原因 -->
          <div v-if="item.status === 'CANCELLED' && item.cancelReason" class="info-row">
            <span class="cancel-info">{{ cancelReasonLabel(item.cancelReason) }}</span>
          </div>
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
          @size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyPickups } from '@/api/pickup'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const activeTab = ref(route.query.role === 'ACCEPTOR' ? 'ACCEPTOR' : 'PUBLISHER')

const statusFilter = ref('')
const list = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const campusMap = {
  GULOU: '鼓楼校区',
  XIANLIN: '仙林校区',
  SUZHOU: '苏州校区',
  PUKOU: '浦口校区'
}
const campusLabel = (code) => campusMap[code] || code

const statusMap = {
  WAITING_ACCEPT: '待接单',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const cancelReasonMap = {
  USER_CANCELLED: '用户取消',
  ACCEPT_DEADLINE_EXPIRED: '接单截止超时',
  SYSTEM_CANCELLED: '系统取消'
}

function getStatusLabel(status) {
  return statusMap[status] || status
}

function cancelReasonLabel(reason) {
  return cancelReasonMap[reason] || reason
}

function getTagType(status) {
  const map = {
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
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function onFilterChange() {
  pagination.page = 1
  loadList()
}

function onPageSizeChange() {
  pagination.page = 1
  loadList()
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
    list.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/pickup/${id}`)
}

onMounted(() => {
  loadList()
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

.desc-preview {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  line-height: 1.4;
  word-break: break-word;
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

.completed-time {
  color: #67c23a;
  font-size: 13px;
}

.cancel-info {
  color: #f56c6c;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>