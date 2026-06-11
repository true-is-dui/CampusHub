<template>
  <div class="notification-list">
    <div class="notification-toolbar">
      <div>
        <div class="notification-heading">消息通知</div>
        <div class="notification-subtitle">{{ pagination.total }} 条通知</div>
      </div>
      <el-button :loading="loading" circle @click="loadNotifications" aria-label="刷新消息通知">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>

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
              <el-tag
                  class="notification-type-tag"
                  :class="`notification-type-tag--${String(item.type || 'default').toLowerCase()}`"
                  size="small"
              >
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
          :pager-count="5"
          small
          layout="prev, pager, next"
          @current-change="loadNotifications"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markRead } from '@/api/notification'

const emit = defineEmits(['navigate'])
const router = useRouter()
const loading = ref(false)
const notifications = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
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
    emit('navigate')
    router.push({ path: `/evaluation/${item.businessId}`, query: { mode: 'received' } })
  } else if (item.type === 'VERIFICATION') {
    emit('navigate')
    router.push('/profile')
  } else if (item.type === 'PAYMENT' && item.businessId) {
    emit('navigate')
    router.push('/points')
  } else if (item.businessType === 'PICKUP_REQUEST' && item.businessId) {
    emit('navigate')
    router.push(`/pickup/${item.businessId}`)
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notification-list {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.notification-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #ebeef5;
}

.notification-heading {
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
}

.notification-subtitle {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.notification-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 2px 8px 0;
}

.notification-item {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
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
  gap: 8px;
  margin-bottom: 8px;
}

.notification-time {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.notification-type-tag {
  --el-tag-bg-color: #f4f4f5;
  --el-tag-border-color: #dcdfe6;
  --el-tag-text-color: #606266;
  font-weight: 600;
}

.notification-type-tag--pickup {
  --el-tag-bg-color: #ecf5ff;
  --el-tag-border-color: #b3d8ff;
  --el-tag-text-color: #337ecc;
}

.notification-type-tag--payment {
  --el-tag-bg-color: #fdf6ec;
  --el-tag-border-color: #f3d19e;
  --el-tag-text-color: #b88230;
}

.notification-type-tag--system {
  --el-tag-bg-color: #f4f4f5;
  --el-tag-border-color: #d3d4d6;
  --el-tag-text-color: #606266;
}

.notification-type-tag--verification {
  --el-tag-bg-color: #f0f9eb;
  --el-tag-border-color: #b3e19d;
  --el-tag-text-color: #529b2e;
}

.notification-type-tag--evaluation {
  --el-tag-bg-color: #fff0f6;
  --el-tag-border-color: #ffbdd6;
  --el-tag-text-color: #d95f8d;
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
  word-break: break-word;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
