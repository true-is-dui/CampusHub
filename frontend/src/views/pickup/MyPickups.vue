<template>
  <div
      class="my-pickups"
      :class="{
        'intro-mode': !sidebarExpanded,
        'sidebar-mode': sidebarExpanded
      }"
  >
    <aside class="pickup-sidebar">
      <section class="role-list" aria-label="订单类别">
        <div
            class="role-option role-option-publisher"
            :class="{ active: activeTab === 'PUBLISHER' }"
            role="button"
            tabindex="0"
            @click="setRole('PUBLISHER')"
            @keydown.enter.prevent="setRole('PUBLISHER')"
            @keydown.space.prevent="setRole('PUBLISHER')"
        >
          <div class="role-title-text">
            我发布的
          </div>
          <div v-if="activeTab === 'PUBLISHER'" class="status-switches" @click.stop>
            <button
                v-for="option in statusOptions"
                :key="option.value || 'ALL'"
                class="status-switch"
                :class="{ active: statusFilter === option.value }"
                type="button"
                @click="setStatus(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div
            class="role-option role-option-acceptor"
            :class="{ active: activeTab === 'ACCEPTOR' }"
            role="button"
            tabindex="0"
            @click="setRole('ACCEPTOR')"
            @keydown.enter.prevent="setRole('ACCEPTOR')"
            @keydown.space.prevent="setRole('ACCEPTOR')"
        >
          <div class="role-title-text">
            我接单的
          </div>
          <div v-if="activeTab === 'ACCEPTOR'" class="status-switches" @click.stop>
            <button
                v-for="option in statusOptions"
                :key="option.value || 'ALL'"
                class="status-switch"
                :class="{ active: statusFilter === option.value }"
                type="button"
                @click="setStatus(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </section>
    </aside>

    <section v-if="sidebarExpanded" class="pickup-content">
      <div class="pickup-scroll">
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
      </div>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="onCurrentPageChange"
            @size-change="onPageSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyPickups } from '@/api/pickup'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const roleValues = ['PUBLISHER', 'ACCEPTOR']
const statusValues = ['WAITING_ACCEPT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
const pageSizeValues = [10, 20, 50]
const myPickupsStateKey = 'campushub:my-pickups:return-state'
const restoredState = readReturnState()

const initialRole = roleValues.includes(route.query.role) ? route.query.role : restoredState?.role || ''
const initialStatus = statusValues.includes(route.query.status) ? route.query.status : restoredState?.status || ''
const initialPage = parsePositiveInt(route.query.page, restoredState?.page || 1)
const initialPageSize = pageSizeValues.includes(Number(route.query.pageSize))
    ? Number(route.query.pageSize)
    : restoredState?.pageSize || 20
const activeTab = ref(initialRole)
const sidebarExpanded = ref(Boolean(initialRole))

const statusFilter = ref(initialStatus)
const list = ref([])

const pagination = reactive({
  page: initialPage,
  pageSize: initialPageSize,
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

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待接单', value: 'WAITING_ACCEPT' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

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

function parsePositiveInt(value, fallback) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function readReturnState() {
  try {
    const raw = sessionStorage.getItem(myPickupsStateKey)
    sessionStorage.removeItem(myPickupsStateKey)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return {
      role: roleValues.includes(parsed.role) ? parsed.role : '',
      status: statusValues.includes(parsed.status) ? parsed.status : '',
      page: parsePositiveInt(parsed.page, 1),
      pageSize: pageSizeValues.includes(Number(parsed.pageSize)) ? Number(parsed.pageSize) : 20
    }
  } catch {
    return null
  }
}

function saveReturnState() {
  if (!activeTab.value) return
  try {
    sessionStorage.setItem(myPickupsStateKey, JSON.stringify({
      role: activeTab.value,
      status: statusFilter.value,
      page: pagination.page,
      pageSize: pagination.pageSize
    }))
  } catch {
    // Storage failure should not block entering the detail page.
  }
}

function onFilterChange() {
  pagination.page = 1
  loadList()
}

function setRole(role) {
  if (activeTab.value === role && sidebarExpanded.value) return
  activeTab.value = role
  pagination.page = 1
  if (!sidebarExpanded.value) {
    sidebarExpanded.value = true
  }
  loadList()
}

function setStatus(status) {
  if (statusFilter.value === status) return
  statusFilter.value = status
  onFilterChange()
}

function onPageSizeChange() {
  pagination.page = 1
  loadList()
}

function onCurrentPageChange() {
  loadList()
}

async function loadList() {
  if (!activeTab.value) return
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
  saveReturnState()
  router.push(`/pickup/${id}`)
}

onMounted(() => {
  if (activeTab.value) {
    loadList()
  }
})

</script>

<style scoped>
.my-pickups {
  display: grid;
  max-width: 1160px;
  margin: 0 auto;
  min-height: 0;
  transition:
      grid-template-columns 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      gap 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      padding-top 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.my-pickups.intro-mode {
  grid-template-columns: 540px minmax(0, 0);
  gap: 0;
  padding-top: 42px;
}

.my-pickups.sidebar-mode {
  grid-template-columns: 300px minmax(0, 720px);
  gap: 136px;
  align-items: stretch;
  height: calc(100vh - 132px);
  overflow: hidden;
}

.pickup-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
  z-index: 3;
  transition:
      transform 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      min-height 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      padding 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      height 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.role-list {
  display: grid;
  gap: 18px;
  transition:
      grid-template-columns 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      gap 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.intro-mode .pickup-sidebar {
  min-height: 0;
  padding: 0;
  transform: translateX(310px);
}

.intro-mode .role-list {
  grid-template-columns: 1fr;
}

.sidebar-mode .pickup-sidebar {
  position: sticky;
  top: 92px;
  height: calc(100vh - 132px);
  justify-content: center;
  padding: 4px 24px 0 0;
  transform: translateX(0);
}

.sidebar-mode .role-list {
  grid-template-columns: 1fr;
  gap: 18px;
}

.role-option {
  min-height: 230px;
  padding: 34px 36px;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  z-index: 3;
  box-sizing: border-box;
  transition:
      width 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      min-height 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      padding 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      transform 0.22s ease,
      box-shadow 0.22s ease,
      opacity 0.22s ease;
  box-shadow: 0 10px 22px rgba(37, 48, 78, 0.12);
}

.role-option::after {
  content: '';
  position: absolute;
  right: -56px;
  bottom: -76px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  pointer-events: none;
}

.role-option:hover {
  transform: translateY(-2px);
}

.role-option.active {
  min-height: 260px;
  z-index: 4;
  box-shadow: 0 16px 30px rgba(37, 48, 78, 0.16);
}

.sidebar-mode .role-option {
  width: 100%;
  min-height: 108px;
  padding: 22px;
}

.sidebar-mode .role-option.active {
  width: calc(100% + 104px);
  min-height: 226px;
  padding: 26px 26px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sidebar-mode .role-option::after {
  right: -42px;
  bottom: -64px;
  width: 132px;
  height: 132px;
}

.sidebar-mode .role-option:not(.active) {
  opacity: 0.56;
}

.role-option-publisher {
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 54%, #ffc0cf 100%);
}

.role-option-acceptor {
  background: linear-gradient(135deg, #4f8cff 0%, #6db7ff 56%, #a5dcff 100%);
}

.role-title-text {
  position: relative;
  z-index: 1;
  width: 100%;
  color: inherit;
  font-size: 34px;
  font-weight: 800;
  line-height: 42px;
  text-align: left;
}

.intro-mode .role-option {
  display: flex;
  align-items: center;
  justify-content: center;
}

.intro-mode .role-title-text {
  font-size: 46px;
  line-height: 56px;
  text-align: center;
}

.role-option.active .role-title-text {
  font-size: 36px;
  line-height: 44px;
}

.sidebar-mode .role-title-text {
  font-size: 24px;
  line-height: 32px;
}

.sidebar-mode .role-option.active .role-title-text {
  font-size: 34px;
  line-height: 42px;
}

.status-switches {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: nowrap;
  gap: 7px;
  margin-top: 22px;
}

.status-switch {
  height: 30px;
  min-width: 54px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  line-height: 28px;
  white-space: nowrap;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.status-switch:hover,
.status-switch.active {
  border-color: rgba(255, 255, 255, 0.86);
  background: rgba(255, 255, 255, 0.92);
  color: #2b3a4a;
}

.pickup-content {
  width: 100%;
  max-width: 720px;
  height: calc(100vh - 132px);
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  position: relative;
  z-index: 1;
  animation: pickupContentIn 0.28s ease both;
}

.pickup-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.pickup-scroll::-webkit-scrollbar {
  width: 6px;
}

.pickup-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #dcdfe6;
}

.pickup-scroll::-webkit-scrollbar-track {
  background: transparent;
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
  min-width: 0;
}

.pickup-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.card-title {
  min-width: 0;
  overflow: hidden;
  color: #303133;
  font-size: 15px;
  font-weight: 500;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-body {
  font-size: 14px;
  color: #606266;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
  min-width: 0;
}

.info-row span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  flex: 0 0 auto;
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
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
  flex: 0 0 auto;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

@keyframes pickupContentIn {
  from {
    opacity: 0;
    transform: translateX(18px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (max-width: 900px) {
  .my-pickups {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .pickup-sidebar {
    position: static;
    min-height: 0;
    padding: 0 0 16px;
    border-right: 0;
    border-bottom: 1px solid #e4e7ed;
  }

  .role-list {
    grid-template-columns: 1fr;
  }

  .role-option.active {
    width: 100%;
    min-height: 148px;
  }
}

@media (max-width: 600px) {
  .my-pickups {
    gap: 14px;
  }

  .pickup-sidebar {
    padding-bottom: 14px;
  }

  .pagination-wrapper {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
