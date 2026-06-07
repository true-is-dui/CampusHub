<template>
  <div class="notification-list">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>消息通知</span>
      </template>
    </el-page-header>

    <div v-loading="loading" class="notification-content">
      <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
      <div
          v-for="item in notifications"
          :key="item.notificationId"
          class="notification-item"
          :class="{ unread: item.readStatus === 'UNREAD' }"
          @click="handleRead(item)"
      >
        <div class="notification-header">
          <div class="notification-type">
            <el-badge :is-dot="item.readStatus === 'UNREAD'" type="danger">
              <el-tag :type="getTypeTag(item.type)" size="small">
                {{ getTypeLabel(item.type) }}
              </el-tag>
            </el-badge>
          </div>
          <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="notification-title">{{ item.title }}</div>
        <div class="notification-body">{{ item.content }}</div>
      </div>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadNotifications"
          @size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markRead } from '@/api/notification'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const typeLabelMap = {
  PICKUP: '代取相关',
  PAYMENT: '支付相关',
  SYSTEM: '系统通知',
  VERIFICATION: '认证通知',
  EVALUATION: '评价通知'
}

function getTypeLabel(type) {
  return typeLabelMap[type] || type || '通知'
}

function getTypeTag(type) {
  const map = {
    PICKUP: 'danger',
    PAYMENT: 'warning',
    SYSTEM: 'info',
    VERIFICATION: 'success',
    EVALUATION: 'info'
  }
  return map[type] || 'info'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function onPageSizeChange() {
  pagination.page = 1
  loadNotifications()
}

async function loadNotifications() {
  loading.value = true
  try {
    const res = await getNotifications({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    notifications.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

// 注入 Layout 提供的刷新未读数方法
const refreshUnreadCount = inject('refreshUnreadCount', null)

async function handleRead(item) {
  if (item.readStatus === 'UNREAD') {
    try {
      await markRead(item.notificationId)
      item.readStatus = 'READ'
      // 立即刷新全局未读数
      if (refreshUnreadCount) refreshUnreadCount()
    } catch {
      // ignore
    }
  }

  // 根据通知类型跳转到关联页面
  if (item.type === 'EVALUATION' && item.businessId) {
    router.push(`/evaluation/${item.businessId}`)
  } else if (item.type === 'VERIFICATION') {
    router.push('/verification')
  } else if (item.type === 'PAYMENT' && item.businessId) {
    router.push('/transactions')
  } else if (item.businessType === 'PICKUP_REQUEST' && item.businessId) {
    router.push(`/pickup/${item.businessId}`)
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notification-list {
  max-width: 800px;
  margin: 0 auto;
}

.notification-content {
  margin-top: 20px;
  min-height: 200px;
}

.notification-item {
  padding: 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f5f7fa;
}

.notification-item.unread {
  background: #ecf5ff;
  border-color: #d9ecff;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-time {
  color: #909399;
  font-size: 12px;
}

.notification-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
}

.notification-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>